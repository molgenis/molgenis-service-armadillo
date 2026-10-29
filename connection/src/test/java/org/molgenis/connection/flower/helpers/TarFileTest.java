package org.molgenis.connection.flower.helpers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

class TarFileTest {

  private static final int PIPE_BUFFER_BYTES = 1024 * 1024;

  @Test
  void getTarStreamReturnsSameNonNullStream() throws IOException {
    TarFile tarFile = new TarFile("file.txt", new ByteArrayInputStream(new byte[0]), 0);

    assertNotNull(tarFile.getTarStream());
    // intended, we want to verify when the method is called twice, both times the same stream will
    // be returned
    assertSame(tarFile.getTarStream(), tarFile.getTarStream());
  }

  @Test
  void writeReturnsNull() throws IOException {
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile = new TarFile("file.txt", new ByteArrayInputStream(content), content.length);

    assertNull(tarFile.write());
  }

  @Test
  void writeProducesTarWithSingleEntryWithNameSizeAndContent() throws IOException {
    byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile = new TarFile("hello.txt", new ByteArrayInputStream(content), content.length);

    // small enough to fit in the pipe buffer, so writing on the same thread does not block
    tarFile.write();

    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(tarFile.getTarStream())) {
      TarArchiveEntry entry = tarIn.getNextEntry();
      assertNotNull(entry);
      assertEquals("hello.txt", entry.getName());
      assertEquals(content.length, entry.getSize());
      assertArrayEquals(content, tarIn.readNBytes((int) entry.getSize()));

      assertNull(tarIn.getNextEntry(), "tar should contain exactly one entry");
    }
  }

  @Test
  void writeSupportsEmptyFile() throws IOException {
    TarFile tarFile = new TarFile("empty.txt", new ByteArrayInputStream(new byte[0]), 0);

    tarFile.write();

    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(tarFile.getTarStream())) {
      TarArchiveEntry entry = tarIn.getNextEntry();
      assertNotNull(entry);
      assertEquals("empty.txt", entry.getName());
      assertEquals(0, entry.getSize());
      assertEquals(0, tarIn.readNBytes(10).length);
      assertNull(tarIn.getNextEntry());
    }
  }

  @Test
  void writeClosesPipeSoReaderSeesEndOfStream() throws IOException {
    byte[] content = "data".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile = new TarFile("file.txt", new ByteArrayInputStream(content), content.length);

    tarFile.write();

    // Drain the pipe; if the output side were not closed this would block/throw instead of
    // returning -1 at the end.
    InputStream tarStream = tarFile.getTarStream();
    byte[] all = tarStream.readAllBytes();
    assertEquals(-1, tarStream.read());
    // Tar data is always a multiple of 512-byte blocks. The exact size depends on the
    // commons-compress version (e.g. optional PAX headers), so don't assert it exactly.
    assertTrue(all.length > 0);
    assertEquals(0, all.length % 512);
  }

  @Test
  void writeAcceptsNameOfMaximumTarLength() throws IOException {
    // the tar name field is 100 bytes, but one is reserved for the terminating NUL
    String name = "a".repeat(99);
    byte[] content = "x".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile = new TarFile(name, new ByteArrayInputStream(content), content.length);

    tarFile.write();

    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(tarFile.getTarStream())) {
      TarArchiveEntry entry = tarIn.getNextEntry();
      assertNotNull(entry);
      assertEquals(name, entry.getName());
    }
  }

  @Test
  void writeRejectsFileNameLongerThanTarLimit() throws IOException {
    String name = "a".repeat(100);
    TarFile tarFile = new TarFile(name, new ByteArrayInputStream(new byte[1]), 1);

    assertThrows(IllegalArgumentException.class, tarFile::write);
  }

  @Test
  void writeStreamsDataLargerThanPipeBuffer() throws Exception {
    // 3 MiB > 1 MiB pipe buffer: write() must run on another thread while we read.
    byte[] content = new byte[3 * PIPE_BUFFER_BYTES + 123];
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) (i % 251);
    }
    TarFile tarFile = new TarFile("big.bin", new ByteArrayInputStream(content), content.length);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      CompletableFuture<Void> writer =
          CompletableFuture.runAsync(
              () -> {
                try {
                  tarFile.write();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              },
              executor);

      try (TarArchiveInputStream tarIn = new TarArchiveInputStream(tarFile.getTarStream())) {
        TarArchiveEntry entry = tarIn.getNextEntry();
        assertNotNull(entry);
        assertEquals("big.bin", entry.getName());
        assertEquals(content.length, entry.getSize());
        assertArrayEquals(content, tarIn.readNBytes(content.length));
        assertNull(tarIn.getNextEntry());
      }

      writer.get(10, TimeUnit.SECONDS); // surfaces any exception from the writer thread
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void writeThrowsWhenDeclaredSizeIsLargerThanData() throws IOException {
    byte[] content = "short".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile =
        new TarFile("file.txt", new ByteArrayInputStream(content), content.length + 10L);

    assertThrows(IOException.class, tarFile::write);
  }

  @Test
  void writeThrowsWhenDeclaredSizeIsSmallerThanData() throws IOException {
    byte[] content = "this is longer than declared".getBytes(StandardCharsets.UTF_8);
    TarFile tarFile = new TarFile("file.txt", new ByteArrayInputStream(content), 4);

    assertThrows(IOException.class, tarFile::write);
  }

  @Test
  void writePropagatesIOExceptionFromDataStream() throws IOException {
    InputStream failing =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("boom");
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("boom");
          }
        };
    TarFile tarFile = new TarFile("file.txt", failing, 10);

    IOException e = assertThrows(IOException.class, tarFile::write);
    assertEquals("boom", e.getMessage());
  }
}
