package org.molgenis.armadillo.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;

public class ArmadilloWorkspace {
  byte[] content;
  public static final String WORKSPACE_TOO_BIG_ERROR = "Unable to load workspace. Maximum supported workspace size is 2GB";

  public ArmadilloWorkspace(InputStream is) {
    content = toByteArray(is);
  }

  private byte[] toByteArray(InputStream is) {
    try {
      return IOUtils.toByteArray(is);
    } catch (OutOfMemoryError e) {
      throw new StorageException(WORKSPACE_TOO_BIG_ERROR);
    } catch (Exception e) {
      throw new StorageException("Unable to load workspace, because: " + e);
    }
  }

  public long getSize() {
    return content.length;
  }

  public ByteArrayInputStream createInputStream() {
    return new ByteArrayInputStream(content);
  }
}
