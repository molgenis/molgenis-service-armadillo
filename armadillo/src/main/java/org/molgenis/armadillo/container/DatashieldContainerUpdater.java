package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DatashieldContainerUpdater implements ContainerUpdater {

  @Override
  public ContainerConfig updateImageMetaData(
      ContainerConfig existingConfig,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {

    if (!(existingConfig instanceof DatashieldContainerConfig)) {
      throw new IllegalArgumentException("Updater only handles DatashieldContainerConfig.");
    }
    DatashieldContainerConfig specificExisting = (DatashieldContainerConfig) existingConfig;

    return DatashieldContainerConfig.create(
        specificExisting.getName(),
        specificExisting.getImage(),
        specificExisting.getAutoUpdate(),
        specificExisting.getUpdateSchedule(),
        specificExisting.getHost(),
        specificExisting.getPort(),
        specificExisting.getPackageWhitelist(),
        specificExisting.getFunctionBlacklist(),
        specificExisting.getOptions(),

        // --- Apply the 6 input parameters ---
        newImageId,
        newVersionId,
        newImageSize,
        newCreationDate,
        newInstallDate != null ? newInstallDate : specificExisting.getInstallDate());
  }
}
