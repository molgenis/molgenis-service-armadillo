package org.molgenis.datashield.exceptions;

import static java.lang.String.format;

import org.obiba.datashield.r.expr.ParseException;

public class DataShieldExpressionException extends RuntimeException {

  public DataShieldExpressionException(ParseException err) {
    super(format("Error parsing expression: %s", err.getMessage()), err);
  }
}