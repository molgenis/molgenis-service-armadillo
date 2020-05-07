package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.obiba.datashield.core.DSMethod;

public class DuplicateRMethodException extends RuntimeException {

  public DuplicateRMethodException(DSMethod dsMethod) {
    super(format("Method already registered: %s", dsMethod.getName()));
  }
}
