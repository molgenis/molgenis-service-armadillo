package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DefaultContainerUpdater implements ContainerUpdater {

  @Override
  public Class<? extends ContainerConfig> supportsConfigType() {
    return DefaultContainerConfig.class; // <-- Identifies its target type
  }

  @Override
  public ContainerConfig updateImageMetaData(
      ContainerConfig existingConfig,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {

    if (!(existingConfig instanceof DefaultContainerConfig)) {
      throw new IllegalArgumentException("Updater only handles DefaultContainerConfig.");
    }
    DefaultContainerConfig specificExisting = (DefaultContainerConfig) existingConfig;

    return specificExisting.toBuilder()
        .lastImageId(newImageId)
        .imageSize(newImageSize)
        .installDate(newInstallDate != null ? newInstallDate : specificExisting.getInstallDate())
        .build();
  }
}
