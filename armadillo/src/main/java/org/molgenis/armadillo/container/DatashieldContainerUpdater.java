package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;
import org.molgenis.armadillo.metadata.OpenContainersImageMetaData;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerUpdater implements ContainerUpdater, OpenContainersUpdater {

  @Override
  public boolean supports(ContainerConfig config) {
    return config instanceof DatashieldContainerConfig;
  }

  @Override
  public ContainerConfig updateDefaultImageMetaData(
      ContainerConfig existingConfig, DefaultImageMetaData metadata) {

    DatashieldContainerConfig specificExisting = (DatashieldContainerConfig) existingConfig;

    return specificExisting.toBuilder()
        .lastImageId(metadata.currentImageId())
        .imageSize(metadata.imageSize())
        .installDate(
            metadata.installDate() != null
                ? metadata.installDate()
                : specificExisting.getInstallDate())
        .build();
  }

  @Override
  public ContainerConfig updateOpenContainersMetaData(
      ContainerConfig existingConfig, OpenContainersImageMetaData metadata) {

    DatashieldContainerConfig specificExisting = (DatashieldContainerConfig) existingConfig;

    return specificExisting.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
