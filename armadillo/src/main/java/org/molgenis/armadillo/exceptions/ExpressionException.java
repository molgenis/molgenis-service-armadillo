package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.r.expr.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ExpressionException extends RuntimeException {

  public ExpressionException(ParseException err) {
    super(format("Error parsing expression: %s", err.getMessage()), err);
  }

  public ExpressionException(NoSuchDSMethodException err) {
    super(err.getMessage(), err);
  }
}
