package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(INTERNAL_SERVER_ERROR)
public class ImageRemoveFailedException extends RuntimeException {
  public ImageRemoveFailedException(String image, String reason) {
    super(format("Error while removing image '%s' because: %s", image, reason));
  }
}
