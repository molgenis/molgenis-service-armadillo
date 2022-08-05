package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

public class DuplicateProjectException extends RuntimeException {

  public DuplicateProjectException(String project) {
    super(format("Project '%s' already exists", project));
  }
}
