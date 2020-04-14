package org.molgenis.datashield.service;

import static java.lang.String.format;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.molgenis.datashield.DataShieldProperties;
import org.molgenis.datashield.exceptions.DuplicateRMethodException;
import org.molgenis.datashield.exceptions.IllegalRMethodStringException;
import org.molgenis.datashield.exceptions.IllegalRPackageException;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
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
  private DataShieldProperties dataShieldProperties;

  private final DSEnvironment aggregateEnvironment;
  private final DSEnvironment assignEnvironment;

  public DataShieldEnvironmentHolderImpl(
      PackageService packageService,
      RConnectionFactory rConnectionFactory,
      DataShieldProperties dataShieldProperties) {
    this.packageService = packageService;
    this.rConnectionFactory = rConnectionFactory;
    this.dataShieldProperties = dataShieldProperties;

    this.aggregateEnvironment = new DataShieldEnvironment(DSMethodType.AGGREGATE);
    this.assignEnvironment = new DataShieldEnvironment(DSMethodType.ASSIGN);
  }

  @PostConstruct
  public void populateEnvironments() throws RserveException, REXPMismatchException {
    List<RPackage> packages = getPackages();
    packages.stream()
        .flatMap(rPackage -> toDsMethods(rPackage.aggregateMethods(), rPackage.name()))
        .filter(dsMethod -> validateMethodIsUnique(dsMethod, aggregateEnvironment))
        .forEach(dsMethod -> addToEnvironment(dsMethod, aggregateEnvironment));
    packages.stream()
        .flatMap(rPackage -> toDsMethods(rPackage.assignMethods(), rPackage.name()))
        .filter(dsMethod -> validateMethodIsUnique(dsMethod, assignEnvironment))
        .forEach(dsMethod -> addToEnvironment(dsMethod, assignEnvironment));
  }

  private List<RPackage> getPackages() throws RserveException, REXPMismatchException {
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

  private Stream<PackagedFunctionDSMethod> toDsMethods(
      ImmutableSet<String> methods, String rPackageName) {
    if (methods != null) {
      validatePackageWhitelisted(rPackageName);
      return methods.stream().map(method -> toDsMethod(rPackageName, method));
    } else {
      return Stream.empty();
    }
  }

  /**
   * Method strings come in two forms: either without a package ('meanDS'), meaning they belong to
   * the current package, or with a name and a package ('dim=base::dim'), meaning they are part of
   * the package described in the string.
   */
  private static PackagedFunctionDSMethod toDsMethod(String packageName, String method) {
    if (method.contains("=")) {
      return toExternalDsMethod(method);
    } else {
      return new PackagedFunctionDSMethod(
          method, format("%s::%s", packageName, method), packageName, null);
    }
  }

  private static PackagedFunctionDSMethod toExternalDsMethod(String method) {
    String[] nonDsBaseMethod = method.split("=");
    if (nonDsBaseMethod.length != 2) {
      throw new IllegalRMethodStringException(method);
    }

    String[] functionParts = nonDsBaseMethod[1].split("::");
    if (functionParts.length != 2) {
      throw new IllegalRMethodStringException(method);
    }

    return new PackagedFunctionDSMethod(
        nonDsBaseMethod[0], nonDsBaseMethod[1], functionParts[0], null);
  }

  private void validatePackageWhitelisted(String rPackageName) {
    if (!dataShieldProperties.getWhitelist().contains(rPackageName)) {
      throw new IllegalRPackageException(rPackageName);
    }
  }

  private boolean validateMethodIsUnique(
      PackagedFunctionDSMethod dsMethod, DSEnvironment environment) {
    if (environment.getMethods().stream()
        .map(DSMethod::getName)
        .anyMatch(name -> dsMethod.getName().equals(name))) {
      throw new DuplicateRMethodException(dsMethod);
    }
    return true;
  }

  private void addToEnvironment(PackagedFunctionDSMethod dsMethod, DSEnvironment environment) {
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
