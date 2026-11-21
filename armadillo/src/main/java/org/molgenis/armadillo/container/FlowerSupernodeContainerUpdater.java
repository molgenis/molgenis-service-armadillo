package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class FlowerSupernodeContainerUpdater
    implements ContainerUpdater<FlowerSuperNodeContainerConfig>,
        OpenContainersUpdater<FlowerSuperNodeContainerConfig> {

  @Override
  public Class<FlowerSuperNodeContainerConfig> getSupportedType() {
    return FlowerSuperNodeContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      FlowerSuperNodeContainerConfig existingConfig, DefaultImageMetadata metadata) {
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
      FlowerSuperNodeContainerConfig existingConfig, OpenContainersImageMetadata metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
