package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfig;
import org.springframework.stereotype.Component;

@Component
public class DatashieldInitialConfigBuilder implements InitialConfigBuilder {

  @Override
  public String getType() {
    return "ds";
  }

  @Override
  public ContainerConfig build(InitialContainerConfig initialConfig) {
    return DatashieldContainerConfig.create(
        initialConfig.getName(),
        initialConfig.getImage(),
        initialConfig.getHost(),
        initialConfig.getPort(),
        null,
        null,
        null,
        null,
        null,
        initialConfig.getAutoUpdate(),
        initialConfig.getUpdateSchedule(),
        initialConfig.getPackageWhitelist(),
        initialConfig.getFunctionBlacklist(),
        initialConfig.getOptions(),
        null, // dockerArgs
        null // dockerOptions
        );
  }
}
