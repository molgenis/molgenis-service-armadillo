package org.molgenis.armadillo.container;

import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class DatashieldRConnectionFactoryProvider {

  public RConnectionFactory create(DatashieldContainerConfig config) {
    return new RServerConnectionFactory(config.toEnvironmentConfigProps());
  }
}
