package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.DefaultImageMetaData;
import org.molgenis.armadillo.metadata.OpenContainersImageMetaData;
import org.springframework.stereotype.Component;

@Component
public class Vantage6ContainerUpdater
    implements ContainerUpdater<Vantage6ContainerConfig>,
        OpenContainersUpdater<Vantage6ContainerConfig> {

  @Override
  public Class<Vantage6ContainerConfig> getSupportedType() {
    return Vantage6ContainerConfig.class;
  }

  @Override
  public ContainerConfig updateDefaultImageMetaData(
      Vantage6ContainerConfig existingConfig, DefaultImageMetaData metadata) {
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
      Vantage6ContainerConfig existingConfig, OpenContainersImageMetaData metadata) {
    return existingConfig.toBuilder()
        .versionId(metadata.openContainersId())
        .creationDate(metadata.creationDate())
        .build();
  }
}
