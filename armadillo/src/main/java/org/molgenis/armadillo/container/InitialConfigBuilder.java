package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfig;

public interface InitialConfigBuilder {

  /** Returns the unique type string this builder handles (e.g., "datashield"). */
  String getType();

  /** Converts the raw config data into the final ContainerConfig object. */
  ContainerConfig build(InitialContainerConfig initialConfig);
}
