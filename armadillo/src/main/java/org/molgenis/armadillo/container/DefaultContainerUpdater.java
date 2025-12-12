package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;
import org.springframework.stereotype.Component;

@Component
public class DefaultContainerUpdater implements ContainerUpdater {

  @Override
  public Class<? extends ContainerConfig> supportsConfigType() {
    return DefaultContainerConfig.class; // <-- Identifies its target type
  }

  @Override
  public ContainerConfig updateDefaultImageMetaData(
      ContainerConfig existingConfig, DefaultImageMetaData metadata) {

    DefaultContainerConfig specificExisting = (DefaultContainerConfig) existingConfig;

    return specificExisting.toBuilder()
        .lastImageId(metadata.currentImageId())
        .imageSize(metadata.imageSize())
        .installDate(
            metadata.installDate() != null
                ? metadata.installDate()
                : specificExisting.getInstallDate())
        .build();
  }
}
