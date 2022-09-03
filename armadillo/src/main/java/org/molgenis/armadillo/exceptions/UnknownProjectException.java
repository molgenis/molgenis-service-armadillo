package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(NOT_FOUND)
public class UnknownProjectException extends RuntimeException {

  public UnknownProjectException(String project) {
    super(format("Project '%s' does not exist", project));
  }
}
