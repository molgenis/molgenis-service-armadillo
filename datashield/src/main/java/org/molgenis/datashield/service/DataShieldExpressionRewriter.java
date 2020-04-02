package org.molgenis.datashield.service;

public interface DataShieldExpressionRewriter {

  String rewriteAssign(String expression);

  String rewriteAggregate(String expression);
}
