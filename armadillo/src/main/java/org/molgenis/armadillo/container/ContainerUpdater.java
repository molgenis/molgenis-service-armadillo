package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;

public interface ContainerUpdater<T extends ContainerConfig> {
  Class<T> getSupportedType();

  default boolean supports(ContainerConfig config) {
    return getSupportedType().isInstance(config);
  }

  ContainerConfig updateDefaultImageMetaData(T existingConfig, DefaultImageMetaData metadata);
}
