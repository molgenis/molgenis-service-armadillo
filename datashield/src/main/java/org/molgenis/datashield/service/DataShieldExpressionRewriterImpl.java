package org.molgenis.datashield.service;

import java.io.StringReader;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.r.expr.DataShieldGrammar;
import org.obiba.datashield.r.expr.ParseException;
import org.obiba.datashield.r.expr.RScriptGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataShieldExpressionRewriterImpl implements DataShieldExpressionRewriter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DataShieldExpressionRewriterImpl.class);

  private final RScriptGenerator rAggregateScriptGenerator;
  private final RScriptGenerator rAssignScriptGenerator;

  public DataShieldExpressionRewriterImpl(DataShieldEnvironmentHolder environmentHolder) {
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
    DataShieldGrammar g = new DataShieldGrammar(new StringReader(expression));

    try {
      String script = rScriptGenerator.toScript(g.root());
      LOGGER.debug("Generated script '{}'", script);
      return script;
    } catch (ParseException e) {
      throw new DataShieldExpressionException(e);
    }
  }
}
