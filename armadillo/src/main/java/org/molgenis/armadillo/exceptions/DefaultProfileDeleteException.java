package org.molgenis.armadillo.exceptions;

import static org.springframework.http.HttpStatus.CONFLICT;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(CONFLICT)
public class DefaultProfileDeleteException extends RuntimeException {

  public DefaultProfileDeleteException() {
    super("The default profile can't be deleted");
  }
}
