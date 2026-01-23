package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class DefaultContainerFactoryImpl implements DefaultContainerFactory {

  @Override
  public String getType() {
    return "default";
  }

  @Override
  public ContainerConfig createDefault() {
    return DefaultContainerConfig.createDefault();
  }
}
