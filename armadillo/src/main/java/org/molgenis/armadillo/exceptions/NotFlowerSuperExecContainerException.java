package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class NotFlowerSuperExecContainerException extends RuntimeException {

  public NotFlowerSuperExecContainerException(String containerName) {
    super(format("Container '%s' is not a Flower SuperExec container", containerName));
  }
}
