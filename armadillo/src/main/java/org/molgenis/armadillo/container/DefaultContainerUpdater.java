package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;
import org.springframework.stereotype.Component;

@Component
public class DefaultContainerUpdater implements ContainerUpdater {

  @Override
  public boolean supports(ContainerConfig config) {
    return config instanceof DefaultContainerConfig;
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
