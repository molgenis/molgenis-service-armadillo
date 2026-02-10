package org.molgenis.armadillo.container;

import org.springframework.stereotype.Component;

@Component
public class Vantage6ContainerFactory implements DefaultContainerFactory {

  @Override
  public String getType() {
    return "v6";
  }

  @Override
  public ContainerConfig createDefault() {
    return Vantage6ContainerConfig.createDefault();
  }
}
