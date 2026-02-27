package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContainerStartStatusTest {

  @Test
  void constructor_setsAllFields() {
    ContainerStartStatus s = new ContainerStartStatus("donkey", "Installing container", 22, 24);

    assertEquals("donkey", s.containerName());
    assertEquals("Installing container", s.status());
    assertEquals(22, s.completedLayers());
    assertEquals(24, s.totalLayers());
  }

  @Test
  void allowsNullValues() {
    ContainerStartStatus s = new ContainerStartStatus(null, null, null, null);

    assertNull(s.containerName());
    assertNull(s.status());
    assertNull(s.completedLayers());
    assertNull(s.totalLayers());
  }

  @Test
  void supportsEdgeNumbers() {
    ContainerStartStatus s =
        new ContainerStartStatus("p", "x", Integer.MAX_VALUE, Integer.MIN_VALUE);

    assertEquals(Integer.MAX_VALUE, s.completedLayers());
    assertEquals(Integer.MIN_VALUE, s.totalLayers());
  }
}
