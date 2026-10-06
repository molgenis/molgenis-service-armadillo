package org.molgenis.connection.flower.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarFile {
  private final PipedOutputStream outputStream;
  private final String fileName;
  private final InputStream data;
  private final long size;

  public TarFile(PipedOutputStream tarOut, String fileName, InputStream data, long size) {
    this.outputStream = tarOut;
    this.fileName = fileName;
    this.data = data;
    this.size = size;
  }

  public Void write() throws IOException {
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
