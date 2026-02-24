package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DefaultContainerFactoryImplTest {

  @Test
  void getType_returnsDefault() {
    DefaultContainerFactoryImpl factory = new DefaultContainerFactoryImpl();

    assertEquals("vanilla", factory.getType());
  }

  @Test
  void createDefault_returnsDefaultConfig() {
    DefaultContainerFactoryImpl factory = new DefaultContainerFactoryImpl();

    ContainerConfig config = factory.createDefault();

    assertNotNull(config);
    assertEquals("vanilla", config.getType());
    assertEquals("default", config.getName());
  }
}
