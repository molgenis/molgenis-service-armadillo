package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfigs;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerFactory implements DefaultContainerFactory {

  private final InitialContainerConfigs config;

  public DatashieldContainerFactory(InitialContainerConfigs config) {
    this.config = config;
  }

  @Override
  public String getType() {
    return "ds";
  }

  @Override
  public ContainerConfig createDefault() {
    return DatashieldContainerConfig.createDefault(
        config.getContainerDefaultImage(), config.getDatashieldDefaultWhitelist());
  }
}
