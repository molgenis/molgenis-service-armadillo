package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.springframework.stereotype.Component;

@Component
public class VanillaContainerUpdater implements ContainerUpdater<VanillaContainerConfig> {

  @Override
  public Class<VanillaContainerConfig> getSupportedType() {
    return VanillaContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetadata(
      VanillaContainerConfig existingConfig, DefaultImageMetadata metadata) {
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
