package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;
import java.util.Map;

public interface ContainerConfig {

  String getName();

  String getImage();

  String getHost();

  Integer getPort();

  String getLastImageId();

  Long getImageSize();

  String getInstallDate();

  @Nullable
  OpenContainersConfig getOpenContainersConfig();

  @Nullable
  UpdatableContainerConfig getUpdatableContainerConfig();

  Map<String, Object> getSpecificContainerConfig();
}
