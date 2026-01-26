package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatashieldContainerFactoryTest {

  @Test
  void getType_returnsDs() {
    DatashieldContainerFactory factory = new DatashieldContainerFactory();

    assertEquals("ds", factory.getType());
  }

  @Test
  void createDefault_requiresDefaultsSet() {
    DatashieldContainerFactory factory = new DatashieldContainerFactory();

    IllegalStateException ex = assertThrows(IllegalStateException.class, factory::createDefault);
    assertEquals("Property \"packageWhitelist\" has not been set", ex.getMessage());
  }
}
