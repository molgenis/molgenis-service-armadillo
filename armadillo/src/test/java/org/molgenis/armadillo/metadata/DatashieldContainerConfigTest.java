package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.armadillo.container.DatashieldContainerConfig.create;

import java.util.HashMap;
import java.util.HashSet;
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
            new HashMap<>());
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
            new HashMap<>());
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
            new HashMap<>());
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
            null);

    assertEquals(Map.of("datashield.seed", "342325352"), config.getDatashieldPrivacyOptions());
  }
}
