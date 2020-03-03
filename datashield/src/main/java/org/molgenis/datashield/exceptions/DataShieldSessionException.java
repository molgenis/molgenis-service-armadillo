package org.molgenis.datashield.exceptions;

import org.rosuda.REngine.Rserve.RserveException;

public class DataShieldSessionException extends RuntimeException {
  public DataShieldSessionException(String message, RserveException err) {
    super(message, err);
  }
}
