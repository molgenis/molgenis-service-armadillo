package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerInterfaceMarkerTest {

  @Test
  void containerConfig_isInterface() {
    assertTrue(ContainerConfig.class.isInterface());
  }

  @Test
  void initialConfigBuilder_isInterface() {
    assertTrue(InitialConfigBuilder.class.isInterface());
  }

  @Test
  void openContainer_isInterface() {
    assertTrue(OpenContainer.class.isInterface());
  }
}
