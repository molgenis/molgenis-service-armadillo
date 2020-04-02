package org.molgenis.datashield.service;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.Package;
import org.molgenis.r.service.PackageService;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.PackagedFunctionDSMethod;
import org.obiba.datashield.r.expr.DataShieldGrammar;
import org.obiba.datashield.r.expr.ParseException;
import org.obiba.datashield.r.expr.RScriptGenerator;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataShieldExpressionRewriterImpl implements DataShieldExpressionRewriter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DataShieldExpressionRewriterImpl.class);
  private final PackageService packageService;
  private final RConnectionFactory rConnectionFactory;

  private final DSEnvironment aggregateEnvironment;
  private final DSEnvironment assignEnvironment;

  private final RScriptGenerator rAggregateScriptGenerator;
  private final RScriptGenerator rAssignScriptGenerator;

  public DataShieldExpressionRewriterImpl(
      PackageService packageService, RConnectionFactory rConnectionFactory) {
    this.packageService = packageService;
    this.rConnectionFactory = rConnectionFactory;

    this.aggregateEnvironment = new DatashieldEnvironment(DSMethodType.AGGREGATE);
    this.assignEnvironment = new DatashieldEnvironment(DSMethodType.ASSIGN);

    this.rAggregateScriptGenerator = new RScriptGenerator(aggregateEnvironment);
    this.rAssignScriptGenerator = new RScriptGenerator(assignEnvironment);
  }

  @PostConstruct
  public void populateEnvironments() throws RserveException, REXPMismatchException {
    RConnection connection = rConnectionFactory.retryCreateConnection();
    List<Package> packages = packageService.getInstalledPackages(connection);

    packages.stream()
        .map(Package::aggregateMethods)
        .filter(Objects::nonNull)
        .flatMap(Set::stream)
        .map(this::toDSMethod)
        .forEach(m -> addToEnvironment(m, aggregateEnvironment));

    packages.stream()
        .map(Package::assignMethods)
        .filter(Objects::nonNull)
        .flatMap(Set::stream)
        .map(this::toDSMethod)
        .forEach(m -> addToEnvironment(m, assignEnvironment));
  }

  /**
   * Method strings come in two forms: either without a package ('meanDS'), meaning they belong to
   * the 'dsBase' package, or with a name and a package ('dim=base::dim'), meaning they are part of
   * another package instead of 'dsBase'.
   */
  private PackagedFunctionDSMethod toDSMethod(String method) {
    if (method.contains("=")) {
      String[] nonDsBaseMethod = method.split("=");
      return new PackagedFunctionDSMethod(nonDsBaseMethod[0], nonDsBaseMethod[1]);
    } else {
      return new PackagedFunctionDSMethod(method, "dsBase::" + method);
    }
  }

  private void addToEnvironment(PackagedFunctionDSMethod dsMethod, DSEnvironment environment) {
    environment.addOrUpdate(dsMethod);
    LOGGER.info(
        "Registered method '{}' to '{}' environment",
        dsMethod.getFunction(),
        environment.getMethodType());
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
