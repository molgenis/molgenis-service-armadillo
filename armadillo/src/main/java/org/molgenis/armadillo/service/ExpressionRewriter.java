package org.molgenis.armadillo.service;

public interface ExpressionRewriter {

  String rewriteAssign(String expression);

  String rewriteAggregate(String expression);
}
