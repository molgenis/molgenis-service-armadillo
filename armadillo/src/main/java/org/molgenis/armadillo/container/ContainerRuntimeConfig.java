package org.molgenis.armadillo.container;

public interface ContainerRuntimeConfig {

  String getName();

  String getImage();

  String getHost();

  Integer getPort();
}
