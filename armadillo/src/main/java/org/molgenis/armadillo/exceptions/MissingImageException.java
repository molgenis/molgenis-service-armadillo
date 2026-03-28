package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class MissingImageException extends RuntimeException {
  public MissingImageException(String container) {
    super(format("Container '%s' does not contain an 'image'", container));
  }
}
