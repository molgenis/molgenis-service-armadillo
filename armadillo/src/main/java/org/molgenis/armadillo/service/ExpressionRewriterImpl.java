package org.molgenis.armadillo.service;

import org.molgenis.armadillo.exceptions.ExpressionException;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.r.expr.RScriptGenerator;
import org.obiba.datashield.r.expr.v2.ParseException;
import org.obiba.datashield.r.expr.v2.RScriptGeneratorV2;
import org.obiba.datashield.r.expr.v2.TokenMgrError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionRewriterImpl implements ExpressionRewriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionRewriterImpl.class);

  private final DSEnvironmentCache environments;

  public ExpressionRewriterImpl(DSEnvironmentCache environments) {
    this.environments = environments;
  }

  @Override
  public String rewriteAssign(String expression) {
    return rewrite(expression, environments.getEnvironment(DSMethodType.ASSIGN));
  }

  @Override
  public String rewriteAggregate(String expression) {
    return rewrite(expression, environments.getEnvironment(DSMethodType.AGGREGATE));
  }

  private String rewrite(String expression, DSEnvironment environment) {
    try {
      RScriptGenerator rScriptGenerator = new RScriptGeneratorV2(environment, expression);
      String script = rScriptGenerator.toScript();
      LOGGER.debug("Generated script '{}'", script);
      return script;
    } catch (ParseException e) {
      throw new ExpressionException(expression, e);
    } catch (NoSuchDSMethodException e) {
      throw new ExpressionException(e);
    } catch (TokenMgrError e) {
      throw new ExpressionException(expression, e);
    }
  }
}
