package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.OpenContainersImageMetaData;

public interface OpenContainersUpdater extends ContainerUpdater {
  ContainerConfig updateOpenContainersMetaData(
      ContainerConfig existingConfig, OpenContainersImageMetaData metadata);
}
