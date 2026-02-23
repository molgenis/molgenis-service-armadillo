package org.molgenis.armadillo.exceptions;

import static org.springframework.http.HttpStatus.CONFLICT;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(CONFLICT)
public class DefaultContainerDeleteException extends RuntimeException {

  public DefaultContainerDeleteException() {
    super("The default container can't be deleted");
  }
}
