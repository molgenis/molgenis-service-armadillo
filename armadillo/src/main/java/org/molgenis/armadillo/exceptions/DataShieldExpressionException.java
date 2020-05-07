package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.r.expr.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataShieldExpressionException extends RuntimeException {

  public DataShieldExpressionException(ParseException err) {
    super(format("Error parsing expression: %s", err.getMessage()), err);
  }

  public DataShieldExpressionException(NoSuchDSMethodException err) {
    super(err.getMessage(), err);
  }
}
