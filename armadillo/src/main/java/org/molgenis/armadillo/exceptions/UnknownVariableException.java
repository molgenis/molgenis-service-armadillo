package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(NOT_FOUND)
public class UnknownVariableException extends RuntimeException {

  public UnknownVariableException(String project, String object, String variable) {
    super(format("Variables ['%s'] do not exist in object '%s/%s'", variable, project, object));
  }
}
