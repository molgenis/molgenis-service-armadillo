package org.molgenis.r.exceptions;

public class ConnectionCreationFailedException extends RuntimeException {
  public ConnectionCreationFailedException(Exception cause) {
    super(cause);
  }

  public ConnectionCreationFailedException(String message) {
    super(message);
  }
}
