package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerUpdater
    implements ContainerUpdater<DatashieldContainerConfig>,
        OpenContainersUpdater<DatashieldContainerConfig> {

  @Override
  public Class<DatashieldContainerConfig> getSupportedType() {
    return DatashieldContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      DatashieldContainerConfig existingConfig, DefaultImageMetadata metadata) {
    return existingConfig.toBuilder()
        .lastImageId(metadata.currentImageId())
        .imageSize(metadata.imageSize())
        .installDate(
            metadata.installDate() != null
                ? metadata.installDate()
                : existingConfig.getInstallDate())
        .build();
  }

  @Override
  public ContainerConfig updateOpenContainersMetaData(
      DatashieldContainerConfig existingConfig, OpenContainersImageMetadata metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
