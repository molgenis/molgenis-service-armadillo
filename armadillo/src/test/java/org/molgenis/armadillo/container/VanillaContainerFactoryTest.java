package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class VanillaContainerFactoryTest {

  @Test
  void getType_returnsDefault() {
    VanillaContainerFactory factory = new VanillaContainerFactory();

    assertEquals("vanilla", factory.getType());
  }

  @Test
  void createDefault_returnsDefaultConfig() {
    VanillaContainerFactory factory = new VanillaContainerFactory();

    ContainerConfig config = factory.createDefault();

    assertNotNull(config);
    assertEquals("vanilla", config.getType());
    assertEquals("default", config.getName());
  }
}
