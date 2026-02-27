package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.InitialConfigBuilder;

class InitialContainerConfigTest {

  @Test
  void toContainerConfig_usesExplicitType() {
    InitialContainerConfig config = new InitialContainerConfig();
    config.setType("ds");
    ContainerConfig expected = mock(ContainerConfig.class);

    InitialConfigBuilder builder =
        new InitialConfigBuilder() {
          @Override
          public String getType() {
            return "ds";
          }

          @Override
          public ContainerConfig build(InitialContainerConfig initialConfig) {
            assertSame(config, initialConfig);
            return expected;
          }
        };

    ContainerConfig result = config.toContainerConfig(Map.of("ds", builder), "default");

    assertSame(expected, result);
  }

  @Test
  void toContainerConfig_usesDefaultTypeWhenMissing() {
    InitialContainerConfig config = new InitialContainerConfig();
    ContainerConfig expected = mock(ContainerConfig.class);

    InitialConfigBuilder builder =
        new InitialConfigBuilder() {
          @Override
          public String getType() {
            return "default";
          }

          @Override
          public ContainerConfig build(InitialContainerConfig initialConfig) {
            assertSame(config, initialConfig);
            return expected;
          }
        };

    ContainerConfig result = config.toContainerConfig(Map.of("default", builder), "default");

    assertSame(expected, result);
  }

  @Test
  void toContainerConfig_throwsWhenBuilderMissing() {
    InitialContainerConfig config = new InitialContainerConfig();
    config.setType("missing");
    Map<String, InitialConfigBuilder> builders = Map.of();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> config.toContainerConfig(builders, "default"));

    assertEquals("No container builder found for type: missing", ex.getMessage());
  }
}
