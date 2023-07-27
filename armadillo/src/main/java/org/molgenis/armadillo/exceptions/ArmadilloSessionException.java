package org.molgenis.armadillo.exceptions;

import org.molgenis.r.RServerException;

public class ArmadilloSessionException extends RuntimeException {
  public ArmadilloSessionException(String message, RServerException err) {
    super(message, err);
  }
}
