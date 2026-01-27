package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfig;

public interface InitialConfigBuilder {

  String getType();

  ContainerConfig build(InitialContainerConfig initialConfig);
}
