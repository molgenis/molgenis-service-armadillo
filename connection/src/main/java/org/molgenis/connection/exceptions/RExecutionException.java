package org.molgenis.connection.exceptions;

public class RExecutionException extends RuntimeException {
  public RExecutionException(Exception ex) {
    super(ex);
  }

  public RExecutionException(String message) {
    super(message);
  }
}
