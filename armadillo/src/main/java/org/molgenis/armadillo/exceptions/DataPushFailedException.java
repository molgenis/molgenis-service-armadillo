package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataPushFailedException extends RuntimeException {
  public DataPushFailedException(String containerName, Throwable cause) {
    super(
        format("Error pushing data to container '%s': %s", containerName, cause.getMessage()),
        cause);
  }
}
