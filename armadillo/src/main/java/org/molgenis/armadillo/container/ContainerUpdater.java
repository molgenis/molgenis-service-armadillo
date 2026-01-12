package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;

public interface ContainerUpdater {
  boolean supports(ContainerConfig config);

  ContainerConfig updateDefaultImageMetaData(
      ContainerConfig existingConfig, DefaultImageMetaData metadata);
}
