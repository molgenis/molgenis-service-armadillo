package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerUpdater implements ContainerUpdater {

  @Override
  public Class<? extends ContainerConfig> supportsConfigType() {
    return DatashieldContainerConfig.class; // <-- Self-identifies its target type
  }

  @Override
  public ContainerConfig updateImageMetaData(
      ContainerConfig existingConfig,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {

    DatashieldContainerConfig specificExisting = (DatashieldContainerConfig) existingConfig;

    return specificExisting.toBuilder()
        .lastImageId(newImageId)
        .versionId(newVersionId)
        .imageSize(newImageSize)
        .creationDate(newCreationDate)
        .installDate(newInstallDate != null ? newInstallDate : specificExisting.getInstallDate())
        .build();
  }
}
