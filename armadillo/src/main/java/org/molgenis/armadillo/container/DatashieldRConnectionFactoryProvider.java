package org.molgenis.armadillo.container;

import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnectionFactory;

public class DatashieldRConnectionFactoryProvider {

  public RConnectionFactory create(DatashieldContainerConfig config) {
    return new RServerConnectionFactory(config.toEnvironmentConfigProps());
  }
}
