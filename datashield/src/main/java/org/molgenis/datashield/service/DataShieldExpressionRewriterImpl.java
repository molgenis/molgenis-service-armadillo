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

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DataShieldExpressionRewriterImpl.class);
  private final PackageService packageService;
  private final RConnectionFactory rConnectionFactory;

  DSEnvironment environment = new DatashieldEnvironment();

  private RScriptGenerator rScriptGenerator;

  public DataShieldExpressionRewriterImpl(PackageService packageService,
      RConnectionFactory rConnectionFactory) {
    this.packageService = packageService;
    this.rConnectionFactory = rConnectionFactory;
    this.rScriptGenerator = new RScriptGenerator(environment);
  }

  @PostConstruct
  public void init() throws RserveException, REXPMismatchException {
    RConnection connection = rConnectionFactory.retryCreateConnection();
    List<Package> packages = packageService.getInstalledPackages(connection);

    packages.stream()
        .map(Package::aggregateMethods)
        .filter(Objects::nonNull)
        .flatMap(Set::stream)
        .map(this::toDSMethod)
        .forEach(this::addToEnvironment);

    packages.stream()
        .map(Package::assignMethods)
        .filter(Objects::nonNull)
        .flatMap(Set::stream)
        .map(this::toDSMethod)
        .forEach(this::addToEnvironment);
  }

  private PackagedFunctionDSMethod toDSMethod(String method) {
    if (method.contains("=")) {
      String[] nonDsBaseMethod = method.split("=");
      return new PackagedFunctionDSMethod(nonDsBaseMethod[0], nonDsBaseMethod[1]);
    } else {
      return new PackagedFunctionDSMethod(method, "dsBase::" + method);
    }
  }

  private void addToEnvironment(PackagedFunctionDSMethod dsMethod) {
    environment.addOrUpdate(dsMethod);
    LOGGER.info("Registered method '{}' to environment", dsMethod.getFunction());
  }

  @Override
  public String rewrite(String expression) {
    DataShieldGrammar g = new DataShieldGrammar(new StringReader(expression));

    try {
      String script = rScriptGenerator.toScript(g.root());
      LOGGER.info("Interpreted expression '{}' as '{}'", expression, script);
      return script;
    } catch (ParseException e) {
      throw new DataShieldExpressionException(e);
    }
  }
}