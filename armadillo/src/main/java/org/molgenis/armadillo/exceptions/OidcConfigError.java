package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class OidcConfigError extends RuntimeException {
  public OidcConfigError(String message) {
    super(format("'%s'", message));
  }
}
