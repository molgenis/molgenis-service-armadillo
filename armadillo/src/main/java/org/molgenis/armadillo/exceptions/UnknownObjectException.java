package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(NOT_FOUND)
public class UnknownObjectException extends RuntimeException {

  public UnknownObjectException(String project, String object) {
    super(format("Project '%s' has no object '%s'", project, object));
  }
}
