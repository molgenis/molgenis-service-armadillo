package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ImageStopFailedException extends RuntimeException {
  public ImageStopFailedException(String image, Throwable cause) {
    super(format("Error stopping image '%s': %S", image, cause.getMessage()), cause);
  }
}
