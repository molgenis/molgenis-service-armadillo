package org.molgenis.r.rock;

import static java.lang.String.format;

import org.molgenis.r.RConnectionVendorFactory;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
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
