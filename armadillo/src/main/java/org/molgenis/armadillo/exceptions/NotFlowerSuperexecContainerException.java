package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class NotFlowerSuperexecContainerException extends RuntimeException {

  public NotFlowerSuperexecContainerException(String containerName) {
    super(format("Container '%s' is not a Flower SuperExec container", containerName));
  }
}
