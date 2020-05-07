package org.molgenis.armadillo.exceptions;

import org.rosuda.REngine.Rserve.RserveException;

public class ArmadilloSessionException extends RuntimeException {
  public ArmadilloSessionException(String message, RserveException err) {
    super(message, err);
  }
}
