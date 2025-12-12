package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;

public interface ContainerUpdater {
  Class<? extends ContainerConfig> supportsConfigType();

  ContainerConfig updateDefaultImageMetaData(
      ContainerConfig existingConfig, DefaultImageMetaData metadata);
}
