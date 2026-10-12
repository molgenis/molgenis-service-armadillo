package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerUpdater
    implements ContainerUpdater<DataShieldContainerConfig>,
        OpenContainersUpdater<DataShieldContainerConfig> {

  @Override
  public Class<DataShieldContainerConfig> getSupportedType() {
    return DataShieldContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
          DataShieldContainerConfig existingConfig, DefaultImageMetadata metadata) {
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
          DataShieldContainerConfig existingConfig, OpenContainersImageMetadata metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
