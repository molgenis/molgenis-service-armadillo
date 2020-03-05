package org.molgenis.datashield.exceptions;

public class StorageException extends RuntimeException {
  public StorageException(Exception cause) {
    super(cause);
  }
}
