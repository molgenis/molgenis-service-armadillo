package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

public class UnknownObjectException extends RuntimeException {

  public UnknownObjectException(String project, String object) {
    super(format("Project '%s' has no object '%s'", project, object));
  }
}
