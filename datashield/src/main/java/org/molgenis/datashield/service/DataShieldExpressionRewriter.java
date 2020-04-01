package org.molgenis.datashield.service;

import org.obiba.datashield.r.expr.ParseException;

public interface DataShieldExpressionRewriter {
  String rewrite(String command) throws ParseException;
}
