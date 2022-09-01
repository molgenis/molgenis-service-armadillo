package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = BAD_REQUEST)
public class IllegalPathException extends RuntimeException {

  public IllegalPathException(String objectName) {
    super(format("Object '%s' is not a valid path", objectName));
  }
}
