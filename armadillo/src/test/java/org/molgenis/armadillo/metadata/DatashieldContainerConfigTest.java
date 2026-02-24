package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.armadillo.container.DatashieldContainerConfig.create;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            host,
            port,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            List.of(),
            Map.of());
    EnvironmentConfigProps actual = config.toEnvironmentConfigProps();
    assertEquals(img, actual.getImage());
  }

  @Test
  public void testToEnvironmentConfigPropsDoesNotThrowErrorWhenImageNull() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            "localhost",
            6311,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            List.of(),
            Map.of());
    assertDoesNotThrow(config::toEnvironmentConfigProps);
  }

  @Test
  public void testCreateEmptyHost() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            null,
            6311,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            List.of(),
            Map.of());
    assertEquals("localhost", config.getHost());
  }

  @Test
  public void testCreateEmptyOptions() {
    DatashieldContainerConfig config =
        create(
            "myName",
            null,
            "localhost",
            6311,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            new HashSet<>(),
            null,
            List.of(),
            Map.of());

    Map<String, String> options = config.getDatashieldROptions();
    assertTrue(options.containsKey("datashield.seed"), "Should have datashield.seed key");
    long seed = Long.parseLong(options.get("datashield.seed"));
    assertTrue(seed >= 0 && seed < 900_000_000L, "Seed should be in valid range");
  }
}
