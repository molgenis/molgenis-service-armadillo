package org.molgenis.datashield.exceptions;

import org.rosuda.REngine.Rserve.RserveException;

public class DatashieldSessionException extends RuntimeException {
  public DatashieldSessionException(String message, RserveException err) {
    super(message, err);
  }
}
