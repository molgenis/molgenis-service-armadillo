package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerWhitelister implements ContainerWhitelister {

  @Override
  public Class<? extends ContainerConfig> supportsConfigType() {
    return DatashieldContainerConfig.class;
  }

  public void addToWhitelist(ContainerConfig config, String pack) {

    if (config instanceof DatashieldContainerConfig dsConfig) {
      dsConfig.getPackageWhitelist().add(pack);
    }
  }
}
