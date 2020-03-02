package org.molgenis.datashield.exceptions;

public class ConnectionCreationFailedException extends RuntimeException {
  public ConnectionCreationFailedException(Exception cause) {
    super(cause);
  }
}
