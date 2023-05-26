package org.molgenis.r.rock;

import static java.lang.String.format;

import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RockConnectionFactory implements RConnectionFactory {

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
    RockApplication application =
        new RockApplication("http://localhost:8085", "administrator", "password");
    try {
      return new RockConnection(application);
    } catch (RServerException e) {
      throw new ConnectionCreationFailedException(e);
    }
  }
}
