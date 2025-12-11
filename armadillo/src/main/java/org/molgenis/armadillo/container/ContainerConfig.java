package org.molgenis.armadillo.container;

import java.util.Map;

public interface ContainerConfig {

  String getName();

  String getImage();

  String getHost();

  Integer getPort();

  String getLastImageId();

  Long getImageSize();

  String getInstallDate();

  Map<String, Object> getSpecificContainerData();
}
