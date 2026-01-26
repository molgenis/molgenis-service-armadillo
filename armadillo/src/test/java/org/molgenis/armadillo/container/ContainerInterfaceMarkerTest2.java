package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerInterfaceMarkerTest2 {

  @Test
  void openContainersUpdater_isInterface() {
    assertTrue(OpenContainersUpdater.class.isInterface());
  }

  @Test
  void updatableContainer_isInterface() {
    assertTrue(UpdatableContainer.class.isInterface());
  }
}
