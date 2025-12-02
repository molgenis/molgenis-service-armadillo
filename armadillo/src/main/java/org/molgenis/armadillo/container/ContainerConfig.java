package org.molgenis.armadillo.container;

public interface ContainerConfig {

  String getName();

  String getImage();

  String getHost();

  Integer getPort();

  String getLastImageId();

  Long getImageSize();

  String getInstallDate();
}
