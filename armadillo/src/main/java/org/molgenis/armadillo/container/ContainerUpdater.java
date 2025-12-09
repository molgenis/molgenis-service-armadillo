package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;

public interface ContainerUpdater {
  ContainerConfig updateImageMetaData(
      ContainerConfig existingConfig,
      String currentImageId,
      String openContainersId, // Specific
      Long imageSize,
      String creationDate, // Specific
      @Nullable String installDate);
}
