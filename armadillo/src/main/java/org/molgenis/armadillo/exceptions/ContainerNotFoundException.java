package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContainerNotFoundException extends RuntimeException {
  public ContainerNotFoundException(String containerName, Throwable cause) {
    super(format("Container '%s' not found", containerName), cause);
  }
}
