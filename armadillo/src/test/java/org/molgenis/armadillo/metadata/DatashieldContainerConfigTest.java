package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.armadillo.container.DatashieldContainerConfig.create;

import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.r.config.EnvironmentConfigProps;

public class DatashieldContainerConfigTest {

  @Test
  public void testToEnvironmentConfigProps() {
    String name = "myName";
    String img = "myImage";
    String host = "localhost";
    int port = 6311;
    DatashieldContainerConfig config =
        create(
            name,
            img,
            false,
            null,
            host,
            port,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            null,
            null,
            null,
            null,
            null);
    EnvironmentConfigProps actual = config.toEnvironmentConfigProps();
    assertEquals(img, actual.getImage());
  }

  @Test
  public void testToEnvironmentConfigPropsDoesNotThrowErrorWhenImageNull() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            false,
            null,
            "localhost",
            6311,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            null,
            null,
            null,
            null,
            null);
    assertDoesNotThrow(config::toEnvironmentConfigProps);
  }

  @Test
  public void testCreateEmptyHost() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            false,
            null,
            null,
            6311,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            null,
            null,
            null,
            null,
            null);
    assertEquals("localhost", config.getHost());
  }

  @Test
  public void testCreateEmptyOptions() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            false,
            null,
            null,
            6311,
            new HashSet<>(),
            new HashSet<>(),
            null,
            null,
            null,
            null,
            null,
            null);
    assertEquals("java.util.ImmutableCollections$MapN", config.getOptions().getClass().getName());
  }
}
