package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;
import org.molgenis.armadillo.metadata.OpenContainersImageMetaData;
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
  public ContainerConfig updateDefaultImageMetaData(
      DatashieldContainerConfig existingConfig, DefaultImageMetaData metadata) {
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
      DatashieldContainerConfig existingConfig, OpenContainersImageMetaData metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
