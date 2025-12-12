package org.molgenis.armadillo.container;

public interface DefaultContainerFactory {
  /**
   * Creates and returns the application's required default container configuration.
   *
   * @return The default ContainerConfig instance.
   */
  ContainerConfig createDefault();
}
