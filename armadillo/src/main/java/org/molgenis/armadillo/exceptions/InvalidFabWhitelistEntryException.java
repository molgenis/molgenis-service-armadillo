package org.molgenis.armadillo.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidFabWhitelistEntryException extends RuntimeException {

  public InvalidFabWhitelistEntryException(String message) {
    super(message);
  }
}
