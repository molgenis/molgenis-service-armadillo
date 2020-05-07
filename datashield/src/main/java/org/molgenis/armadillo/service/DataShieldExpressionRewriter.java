package org.molgenis.armadillo.service;

public interface DataShieldExpressionRewriter {

  String rewriteAssign(String expression);

  String rewriteAggregate(String expression);
}
