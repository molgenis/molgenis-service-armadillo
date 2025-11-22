package org.molgenis.armadillo.service;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Stream;
import org.molgenis.armadillo.container.annotation.ProfileScope;
import org.molgenis.armadillo.exceptions.DuplicateRMethodException;
import org.molgenis.armadillo.exceptions.IllegalRMethodStringException;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.DefaultDSMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Caches the datashield environments for one container. */
@Component
@ProfileScope
public class DSEnvironmentCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(DSEnvironmentCache.class);
  private final PackageService packageService;
  private final RConnectionFactory rConnectionFactory;
  private final ContainerConfig containerConfig;

  private final DSEnvironment aggregateEnvironment;
  private final DSEnvironment assignEnvironment;

  public DSEnvironmentCache(
      PackageService packageService,
      RConnectionFactory rConnectionFactory,
      ContainerConfig containerConfig) {
    this.packageService = requireNonNull(packageService);
    this.rConnectionFactory = requireNonNull(rConnectionFactory);
    this.containerConfig = requireNonNull(containerConfig);

    this.aggregateEnvironment = new DataShieldEnvironment(DSMethodType.AGGREGATE);
    this.assignEnvironment = new DataShieldEnvironment(DSMethodType.ASSIGN);
  }

  @PostConstruct
  public void populateEnvironments() {
    List<RPackage> packages = getPackages();
    packages.stream()
        .flatMap(rPackage -> toDsMethods(rPackage.aggregateMethods(), rPackage))
        .filter(dsMethod -> validateMethodIsUnique(dsMethod, aggregateEnvironment))
        .filter(this::isMethodAllowed)
        .forEach(dsMethod -> addToEnvironment(dsMethod, aggregateEnvironment));
    packages.stream()
        .flatMap(rPackage -> toDsMethods(rPackage.assignMethods(), rPackage))
        .filter(dsMethod -> validateMethodIsUnique(dsMethod, assignEnvironment))
        .filter(this::isMethodAllowed)
        .forEach(dsMethod -> addToEnvironment(dsMethod, assignEnvironment));
  }

  private List<RPackage> getPackages() {
    RServerConnection connection = null;
    try {
      connection = rConnectionFactory.tryCreateConnection();
      return packageService.getInstalledPackages(connection);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  private Stream<DefaultDSMethod> toDsMethods(ImmutableSet<String> methods, RPackage rPackage) {
    if (methods != null && isPackageWhitelisted(rPackage.name())) {
      return methods.stream().map(method -> toDsMethod(rPackage, method));
    }
    return Stream.empty();
  }

  /**
   * Method strings come in two forms: either without a package ('meanDS'), meaning they belong to
   * the current package, or with a name and a package ('dim=base::dim'), meaning they are part of
   * the package described in the string.
   */
  private static DefaultDSMethod toDsMethod(RPackage rPackage, String method) {
    if (method.contains("=")) {
      return toExternalDsMethod(method);
    } else {
      return new DefaultDSMethod(
          method, format("%s::%s", rPackage.name(), method), rPackage.name(), rPackage.version());
    }
  }

  private static DefaultDSMethod toExternalDsMethod(String method) {
    String[] nonDsBaseMethod = method.split("=");
    if (nonDsBaseMethod.length != 2) {
      throw new IllegalRMethodStringException(method);
    }

    String[] functionParts = nonDsBaseMethod[1].split("::");
    if (functionParts.length != 2) {
      throw new IllegalRMethodStringException(method);
    }

    return new DefaultDSMethod(nonDsBaseMethod[0], nonDsBaseMethod[1], functionParts[0], null);
  }

  private boolean isPackageWhitelisted(String rPackageName) {
    if (!containerConfig.getPackageWhitelist().contains(rPackageName)) {
      LOGGER.warn(
          "Package '{}' is not whitelisted and will not be added to environment", rPackageName);
      return false;
    }
    return true;
  }

  private boolean validateMethodIsUnique(DefaultDSMethod dsMethod, DSEnvironment environment) {
    if (environment.getMethods().stream()
        .map(DSMethod::getName)
        .anyMatch(name -> dsMethod.getName().equals(name))) {
      throw new DuplicateRMethodException(dsMethod);
    }
    return true;
  }

  private boolean isMethodAllowed(DefaultDSMethod dsMethod) {
    if (containerConfig.getFunctionBlacklist().contains(dsMethod.getName())) {
      LOGGER.warn(
          "Method '{}' in package '{}' is blacklisted and will not be added to environment",
          dsMethod.getName(),
          dsMethod.getPackage());
      return false;
    }
    return true;
  }

  private void addToEnvironment(DefaultDSMethod dsMethod, DSEnvironment environment) {
    environment.addOrUpdate(dsMethod);
    LOGGER.info(
        "Registered method '{}' to '{}' environment",
        dsMethod.getFunction(),
        environment.getMethodType());
  }

  public DSEnvironment getEnvironment(DSMethodType dsMethodType) {
    return switch (dsMethodType) {
      case AGGREGATE -> aggregateEnvironment;
      case ASSIGN -> assignEnvironment;
    };
  }
}
