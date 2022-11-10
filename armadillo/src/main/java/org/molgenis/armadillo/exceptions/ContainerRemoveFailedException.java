package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(INTERNAL_SERVER_ERROR)
public class ContainerRemoveFailedException extends RuntimeException {
  public ContainerRemoveFailedException(String container, Throwable cause) {
    super(format("Error while removing container '%s'", container), cause);
  }
}
