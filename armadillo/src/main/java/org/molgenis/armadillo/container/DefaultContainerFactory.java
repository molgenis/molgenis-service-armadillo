package org.molgenis.armadillo.container;

public interface DefaultContainerFactory {
  String getType();

  ContainerConfig createDefault();
}
