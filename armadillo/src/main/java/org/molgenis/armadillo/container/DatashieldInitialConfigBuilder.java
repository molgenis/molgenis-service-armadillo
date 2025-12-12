package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfig;
import org.springframework.stereotype.Component;

@Component
public class DatashieldInitialConfigBuilder implements InitialConfigBuilder {

  @Override
  public String getType() {
    return "datashield";
  }

  @Override
  public ContainerConfig build(InitialContainerConfig initialConfig) {
    return DatashieldContainerConfig.create(
        initialConfig.getName(),
        initialConfig.getImage(),
        initialConfig.getAutoUpdate(),
        initialConfig.getUpdateSchedule(),
        initialConfig.getHost(),
        initialConfig.getPort(),
        initialConfig.getPackageWhitelist(),
        initialConfig.getFunctionBlacklist(),
        initialConfig.getOptions(),
        null,
        null,
        null,
        null,
        null);
  }
}
