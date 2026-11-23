package org.molgenis.armadillo.exceptions;

public class RebootScriptRunFailedException extends RuntimeException {

  public RebootScriptRunFailedException(String message, Exception cause) {
    super("Reboot script failed: " + message, cause);
  }
}
