package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class FlowerSuperexecContainerUpdater
    implements ContainerUpdater<FlowerSuperExecContainerConfig>,
        OpenContainersUpdater<FlowerSuperExecContainerConfig> {

  @Override
  public Class<FlowerSuperExecContainerConfig> getSupportedType() {
    return FlowerSuperExecContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      FlowerSuperExecContainerConfig existingConfig, DefaultImageMetadata metadata) {
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
      FlowerSuperExecContainerConfig existingConfig, OpenContainersImageMetadata metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
