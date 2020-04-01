package org.molgenis.datashield.service;

import org.obiba.datashield.r.expr.ParseException;

public interface DataShieldExpressionRewriter {

  String rewriteAssign(String expression) throws ParseException;

  String rewriteAggregate(String expression) throws ParseException;
}
