package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(NOT_FOUND)
public class UnknownUserException extends RuntimeException {

  public UnknownUserException(String email) {
    super(format("User '%s' does not exist", email));
  }
}
