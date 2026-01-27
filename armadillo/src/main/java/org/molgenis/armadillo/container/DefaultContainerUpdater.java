package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class DefaultContainerUpdater implements ContainerUpdater<DefaultContainerConfig> {

  @Override
  public Class<DefaultContainerConfig> getSupportedType() {
    return DefaultContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      DefaultContainerConfig existingConfig, DefaultImageMetadata metadata) {
    return existingConfig.toBuilder()
        .lastImageId(metadata.currentImageId())
        .imageSize(metadata.imageSize())
        .installDate(
            metadata.installDate() != null
                ? metadata.installDate()
                : existingConfig.getInstallDate())
        .build();
  }
}
