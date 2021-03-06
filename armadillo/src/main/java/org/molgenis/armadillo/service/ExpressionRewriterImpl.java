package org.molgenis.armadillo.service;

import java.io.StringReader;
import org.molgenis.armadillo.exceptions.ExpressionException;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.r.expr.DataShieldGrammar;
import org.obiba.datashield.r.expr.ParseException;
import org.obiba.datashield.r.expr.RScriptGenerator;
import org.obiba.datashield.r.expr.TokenMgrError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExpressionRewriterImpl implements ExpressionRewriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionRewriterImpl.class);

  private final RScriptGenerator rAggregateScriptGenerator;
  private final RScriptGenerator rAssignScriptGenerator;

  public ExpressionRewriterImpl(DataShieldEnvironmentHolder environmentHolder) {
    this.rAggregateScriptGenerator =
        new RScriptGenerator(environmentHolder.getEnvironment(DSMethodType.AGGREGATE));
    this.rAssignScriptGenerator =
        new RScriptGenerator(environmentHolder.getEnvironment(DSMethodType.ASSIGN));
  }

  @Override
  public String rewriteAssign(String expression) {
    return rewrite(expression, rAssignScriptGenerator);
  }

  @Override
  public String rewriteAggregate(String expression) {
    return rewrite(expression, rAggregateScriptGenerator);
  }

  private String rewrite(String expression, RScriptGenerator rScriptGenerator) {
    try {
      DataShieldGrammar g = new DataShieldGrammar(new StringReader(expression));
      String script = rScriptGenerator.toScript(g.root());
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
