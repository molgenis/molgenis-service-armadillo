package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerFactory implements DefaultContainerFactory {

  @Override
  public ContainerConfig createDefault() {
    return DatashieldContainerConfig.createDefault();
  }
}
