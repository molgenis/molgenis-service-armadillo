package org.molgenis.armadillo.container;

import org.molgenis.r.config.EnvironmentConfigProps;

public abstract class AbstractContainerConfig implements ContainerConfig {

  public abstract String getName();

  public abstract String getImage();

  public abstract String getHost();

  public abstract Integer getPort();

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    props.setImage(getImage());
    return props;
  }
}
