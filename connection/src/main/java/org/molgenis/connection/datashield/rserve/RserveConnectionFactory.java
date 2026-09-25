package org.molgenis.connection.datashield.rserve;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import org.molgenis.connection.config.EnvironmentConfigProps;
import org.molgenis.connection.datashield.RConnectionVendorFactory;
import org.molgenis.connection.datashield.RServerConnection;
import org.molgenis.connection.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RserveConnectionFactory implements RConnectionVendorFactory {

  private static final Logger logger = LoggerFactory.getLogger(RserveConnectionFactory.class);

  private final EnvironmentConfigProps environment;

  public RserveConnectionFactory(EnvironmentConfigProps environment) {
    this.environment = requireNonNull(environment);
  }

  @Override
  public RServerConnection tryCreateConnection() {
    if (logger.isDebugEnabled()) {
      logger.debug(
          format(
              "Trying to connect to instance: [ %s ] on [ %s ]",
              environment.getHost(), environment.getPort()));
    }
    try {
      RConnection rConnection = newConnection(environment.getHost(), environment.getPort());
      if (logger.isDebugEnabled()) {
        logger.debug(
            format(
                "Connected to instance: [ %s ] on [ %s ]",
                environment.getHost(), environment.getPort()));
      }
      return new RserveConnection(rConnection);
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  @VisibleForTesting
  public RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }

  @Override
  public String toString() {
    return "RConnectionFactoryImpl{" + "environment=" + environment.getName() + '}';
  }
}
