package org.molgenis.connection.datashield.rock;

import static java.lang.String.format;

import org.molgenis.connection.config.EnvironmentConfigProps;
import org.molgenis.connection.datashield.RConnectionVendorFactory;
import org.molgenis.connection.datashield.RServerConnection;
import org.molgenis.connection.datashield.RServerException;
import org.molgenis.connection.exceptions.ConnectionCreationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RockConnectionFactory implements RConnectionVendorFactory {

  private static final Logger logger = LoggerFactory.getLogger(RockConnectionFactory.class);
  private final EnvironmentConfigProps environment;

  public RockConnectionFactory(EnvironmentConfigProps environment) {
    this.environment = environment;
  }

  @Override
  public RServerConnection tryCreateConnection() {
    if (logger.isDebugEnabled()) {
      logger.debug(
          format(
              "Trying to connect to instance: [ %s ] on [ %s ]",
              environment.getHost(), environment.getPort()));
    }
    // TODO make rock credentials configurable
    RockApplication application =
        new RockApplication(
            format("http://%s:%s", environment.getHost(), environment.getPort()),
            "administrator",
            "password");
    try {
      return new RockConnection(application);
    } catch (RServerException e) {
      throw new ConnectionCreationFailedException(e);
    }
  }
}
