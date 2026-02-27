package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;

public interface OpenContainersUpdater<T extends ContainerConfig> extends ContainerUpdater<T> {
  ContainerConfig updateOpenContainersMetaData(
      T existingConfig, OpenContainersImageMetadata metadata);
}
