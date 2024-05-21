package org.molgenis.armadillo.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;

public class ArmadilloWorkspace {
  byte[] content;

  public ArmadilloWorkspace(InputStream is) {
    content = toByteArray(is);
  }

  private byte[] toByteArray(InputStream is) {
    try {
      return IOUtils.toByteArray(is);
    } catch (IOException e) {
      throw new StorageException("Unable to read workspace");
    }
  }

  public long getSize() {
    return content.length;
  }

  public ByteArrayInputStream createInputStream() {
    return new ByteArrayInputStream(content);
  }
}
