package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.armadillo.metadata.ProfileConfig.create;

import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.molgenis.r.config.EnvironmentConfigProps;

public class ProfileConfigTest {

  @Test
  public void testToEnvironmentConfigProps() {
    String name = "myName";
    String img = "myImage";
    String host = "localhost";
    int port = 6311;
    ProfileConfig config =
        create(name, img, host, port, new HashSet<>(), new HashSet<>(), new HashMap<>());
    EnvironmentConfigProps actual = config.toEnvironmentConfigProps();
    assertEquals(img, actual.getImage());
  }

  @Test
  public void testToEnvironmentConfigPropsDoesNotThrowErrorWhenImageNull() {
    ProfileConfig config =
        create(
            "myName", null, "localhost", 6311, new HashSet<>(), new HashSet<>(), new HashMap<>());
    assertDoesNotThrow(config::toEnvironmentConfigProps);
  }

  @Test
  public void testCreateEmptyHost() {
    ProfileConfig config =
        create("myName", null, null, 6311, new HashSet<>(), new HashSet<>(), new HashMap<>());
    assertEquals("localhost", config.getHost());
  }

  @Test
  public void testCreateEmptyOptions() {
    ProfileConfig config =
        create("myName", null, null, 6311, new HashSet<>(), new HashSet<>(), null);
    assertEquals("java.util.ImmutableCollections$MapN", config.getOptions().getClass().getName());
  }
}
