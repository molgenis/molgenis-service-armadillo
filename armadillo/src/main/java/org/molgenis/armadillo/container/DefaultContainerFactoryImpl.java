package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class DefaultContainerFactoryImpl implements DefaultContainerFactory {

  @Override
  public String getType() {
    return "vanilla";
  }

  @Override
  public ContainerConfig createDefault() {
    return DefaultContainerConfig.createDefault();
  }
}
