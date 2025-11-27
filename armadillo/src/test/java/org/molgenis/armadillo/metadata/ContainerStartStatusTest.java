package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContainerStartStatusTest {

  @Test
  void constructor_setsAllFields() {
    ContainerStartStatus s = new ContainerStartStatus("donkey", "Installing container", 22, 24);

    assertEquals("donkey", s.getContainerName());
    assertEquals("Installing container", s.getStatus());
    assertEquals(22, s.getCompletedLayers());
    assertEquals(24, s.getTotalLayers());
  }

  @Test
  void setters_updateFields() {
    ContainerStartStatus s = new ContainerStartStatus(null, null, null, null);

    s.setContainerName("shrek");
    s.setStatus("Profile installed");
    s.setCompletedLayers(24);
    s.setTotalLayers(24);

    assertAll(
        () -> assertEquals("shrek", s.getContainerName()),
        () -> assertEquals("Profile installed", s.getStatus()),
        () -> assertEquals(24, s.getCompletedLayers()),
        () -> assertEquals(24, s.getTotalLayers()));
  }

  @Test
  void allowsNullValues() {
    ContainerStartStatus s = new ContainerStartStatus(null, null, null, null);

    assertNull(s.getContainerName());
    assertNull(s.getStatus());
    assertNull(s.getCompletedLayers());
    assertNull(s.getTotalLayers());

    // Set to non-null then back to null
    s.setContainerName("name");
    s.setStatus("status");
    s.setCompletedLayers(1);
    s.setTotalLayers(2);

    s.setContainerName(null);
    s.setStatus(null);
    s.setCompletedLayers(null);
    s.setTotalLayers(null);

    assertAll(
        () -> assertNull(s.getContainerName()),
        () -> assertNull(s.getStatus()),
        () -> assertNull(s.getCompletedLayers()),
        () -> assertNull(s.getTotalLayers()));
  }

  @Test
  void supportsEdgeNumbers() {
    ContainerStartStatus s =
        new ContainerStartStatus("p", "x", Integer.MAX_VALUE, Integer.MIN_VALUE);

    assertEquals(Integer.MAX_VALUE, s.getCompletedLayers());
    assertEquals(Integer.MIN_VALUE, s.getTotalLayers());
  }
}
