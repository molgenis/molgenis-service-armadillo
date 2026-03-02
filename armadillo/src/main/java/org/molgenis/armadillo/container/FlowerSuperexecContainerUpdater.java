package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class FlowerSuperexecContainerUpdater
    implements ContainerUpdater<FlowerSuperexecContainerConfig>,
        OpenContainersUpdater<FlowerSuperexecContainerConfig> {

  @Override
  public Class<FlowerSuperexecContainerConfig> getSupportedType() {
    return FlowerSuperexecContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      FlowerSuperexecContainerConfig existingConfig, DefaultImageMetadata metadata) {
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
      FlowerSuperexecContainerConfig existingConfig, OpenContainersImageMetadata metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
