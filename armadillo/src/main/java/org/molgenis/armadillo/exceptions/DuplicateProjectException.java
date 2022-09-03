package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.CONFLICT;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(CONFLICT)
public class DuplicateProjectException extends RuntimeException {

  public DuplicateProjectException(String project) {
    super(format("Project '%s' already exists", project));
  }
}
