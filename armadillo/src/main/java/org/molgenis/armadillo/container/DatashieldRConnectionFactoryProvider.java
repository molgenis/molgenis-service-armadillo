package org.molgenis.armadillo.container;

import org.molgenis.connection.datashield.RConnectionFactory;
import org.molgenis.connection.datashield.RServerConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class DatashieldRConnectionFactoryProvider {

  public RConnectionFactory create(DatashieldContainerConfig config) {
    return new RServerConnectionFactory(config.toEnvironmentConfigProps());
  }
}
