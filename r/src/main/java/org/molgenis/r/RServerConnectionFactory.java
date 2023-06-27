package org.molgenis.r;

import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.rock.RockConnectionFactory;
import org.molgenis.r.rserve.RserveConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RServerConnectionFactory implements RConnectionFactory {
  private static final Logger logger = LoggerFactory.getLogger(RServerConnectionFactory.class);

  private final EnvironmentConfigProps environment;

  public RServerConnectionFactory(EnvironmentConfigProps environment) {
    this.environment = environment;
  }

  @Override
  public RServerConnection tryCreateConnection() {
    try {
      if (environment.getName().contains("rock")) {
        return new RockConnectionFactory(environment).tryCreateConnection();
      } else {
        return new RserveConnectionFactory(environment).tryCreateConnection();
      }
    } catch (Exception e) {
      logger.info("Not a Rock server [{}], trying Rserve...", e.getMessage(), e);
      return new RserveConnectionFactory(environment).tryCreateConnection();
    }
  }
}
