package org.molgenis.connection.flower.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarFile {
  private final PipedOutputStream outputStream;
  private final String fileName;
  private final InputStream data;
  private final long size;
  private final PipedInputStream tarStream;
  private static final int PIPE_BUFFER_BYTES = 1024 * 1024;

  public TarFile(String fileName, InputStream data, long size) throws IOException {
    this.tarStream = new PipedInputStream(PIPE_BUFFER_BYTES);
    this.outputStream = new PipedOutputStream(tarStream);
    ;
    this.fileName = fileName;
    this.data = data;
    this.size = size;
  }

  public PipedInputStream getTarStream() {
    return this.tarStream;
  }

  public Void write() throws IOException {
    // The tar is written on a separate thread into a pipe that Docker reads from, so memory use
    // stays at one pipe buffer regardless of file size.
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(this.outputStream)) {
      tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
      TarArchiveEntry entry = new TarArchiveEntry(fileName);
      entry.setSize(size);
      tar.putArchiveEntry(entry);
      data.transferTo(tar);
      tar.closeArchiveEntry();
    }
    return null;
  }
}
