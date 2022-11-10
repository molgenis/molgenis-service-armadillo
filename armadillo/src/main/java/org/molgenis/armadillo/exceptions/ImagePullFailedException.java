package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ImagePullFailedException extends RuntimeException {
  public ImagePullFailedException(String image, Throwable cause) {
    super(format("Error while pulling image '%s'", image), cause);
  }
}
