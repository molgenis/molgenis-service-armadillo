package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

public class UnknownProjectException extends RuntimeException {

  public UnknownProjectException(String project) {
    super(format("Project '%s' does not exist", project));
  }
}
