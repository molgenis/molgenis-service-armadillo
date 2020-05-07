package org.molgenis.armadillo.exceptions;

public class StorageException extends RuntimeException {
  public StorageException(Exception cause) {
    super(cause);
  }
}
