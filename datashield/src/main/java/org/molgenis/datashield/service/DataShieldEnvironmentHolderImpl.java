package org.molgenis.datashield.service;

import static java.lang.String.format;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.Package;
import org.molgenis.r.service.PackageService;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.PackagedFunctionDSMethod;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataShieldEnvironmentHolderImpl implements DataShieldEnvironmentHolder {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DataShieldEnvironmentHolderImpl.class);
  private final PackageService packageService;
  private final RConnectionFactory rConnectionFactory;

  private final DSEnvironment aggregateEnvironment;
  private final DSEnvironment assignEnvironment;

  public DataShieldEnvironmentHolderImpl(
      PackageService packageService, RConnectionFactory rConnectionFactory) {
    this.packageService = packageService;
    this.rConnectionFactory = rConnectionFactory;

    this.aggregateEnvironment = new DataShieldEnvironment(DSMethodType.AGGREGATE);
    this.assignEnvironment = new DataShieldEnvironment(DSMethodType.ASSIGN);
  }

  @PostConstruct
  public void populateEnvironments() throws RserveException, REXPMismatchException {
    List<Package> packages = getPackages();

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

  private List<Package> getPackages() throws RserveException, REXPMismatchException {
    RConnection connection = null;
    try {
      connection = rConnectionFactory.retryCreateConnection();
      return packageService.getInstalledPackages(connection);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  /**
   * Method strings come in two forms: either without a package ('meanDS'), meaning they belong to
   * the 'dsBase' package, or with a name and a package ('dim=base::dim'), meaning they are part of
   * another package instead of 'dsBase'.
   */
  private PackagedFunctionDSMethod toDSMethod(String method) {
    if (method.contains("=")) {
      String[] nonDsBaseMethod = method.split("=");
      if (nonDsBaseMethod.length != 2) {
        throw new IllegalArgumentException(method);
      }
      return new PackagedFunctionDSMethod(nonDsBaseMethod[0], nonDsBaseMethod[1]);
    } else {
      return new PackagedFunctionDSMethod(method, "dsBase::" + method);
    }
  }

  private void addToEnvironment(PackagedFunctionDSMethod dsMethod, DSEnvironment environment) {
    if (environment.getMethods().stream()
        .map(DSMethod::getName)
        .anyMatch(name -> dsMethod.getName().equals(name))) {
      throw new IllegalArgumentException(
          format("Method name already registered: %s", dsMethod.getName()));
    }

    environment.addOrUpdate(dsMethod);
    LOGGER.info(
        "Registered method '{}' to '{}' environment",
        dsMethod.getFunction(),
        environment.getMethodType());
  }

  @Override
  public DSEnvironment getEnvironment(DSMethodType dsMethodType) {
    switch (dsMethodType) {
      case AGGREGATE:
        return aggregateEnvironment;
      case ASSIGN:
        return assignEnvironment;
      default:
        throw new IllegalStateException("Unknown DSMethodType");
    }
  }
}
