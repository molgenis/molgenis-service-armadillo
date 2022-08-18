package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.CONFLICT;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(CONFLICT)
public class DuplicateObjectException extends RuntimeException {

  public DuplicateObjectException(String project, String object) {
    super(format("Project '%s' already has an object '%s'", project, object));
  }
}
