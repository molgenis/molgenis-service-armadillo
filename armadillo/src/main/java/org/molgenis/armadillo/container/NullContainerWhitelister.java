package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class NullContainerWhitelister implements ContainerWhitelister {

  @Override
  public Class<? extends ContainerConfig> supportsConfigType() {
    return DefaultContainerConfig.class;
  }

  @Override
  public void addToWhitelist(ContainerConfig config, String pack) {}
}
