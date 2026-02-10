package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class UnknownVantage6TokenException extends RuntimeException {

  public UnknownVantage6TokenException(String tokenId) {
    super(format("Vantage6 token not found: %s", tokenId));
  }
}
