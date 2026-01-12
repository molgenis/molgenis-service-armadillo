package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class NullContainerWhitelister implements ContainerWhitelister {

  @Override
  public boolean supports(ContainerConfig config) {
    return config instanceof DefaultContainerConfig;
  }

  @Override
  public void addToWhitelist(ContainerConfig config, String pack) {}
}
