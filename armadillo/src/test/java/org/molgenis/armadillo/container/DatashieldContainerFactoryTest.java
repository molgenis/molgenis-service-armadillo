package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.InitialContainerConfigs;

class DatashieldContainerFactoryTest {

  @Test
  void getType_returnsDs() {
    InitialContainerConfigs config = mock(InitialContainerConfigs.class);
    DatashieldContainerFactory factory = new DatashieldContainerFactory(config);

    assertEquals("ds", factory.getType());
  }

  @Test
  void createDefault_usesConfigValues() {
    InitialContainerConfigs config = mock(InitialContainerConfigs.class);
    when(config.getContainerDefaultImage()).thenReturn("test/image");
    when(config.getDatashieldDefaultWhitelist()).thenReturn(Set.of("dsBase", "dsTidyverse"));
    when(config.getDatashieldDefaultBlacklist()).thenReturn(Set.of("someBlockedFunc"));

    DatashieldContainerFactory factory = new DatashieldContainerFactory(config);
    DatashieldContainerConfig result = (DatashieldContainerConfig) factory.createDefault();

    assertEquals("default", result.getName());
    assertEquals("test/image", result.getImage());
    assertEquals(Set.of("dsBase", "dsTidyverse"), result.getPackageWhitelist());
    assertEquals(Set.of("someBlockedFunc"), result.getFunctionBlacklist());
  }

  @Test
  void createDefault_usesFallbacksWhenConfigNull() {
    InitialContainerConfigs config = mock(InitialContainerConfigs.class);
    when(config.getContainerDefaultImage()).thenReturn(null);
    when(config.getDatashieldDefaultWhitelist()).thenReturn(null);
    when(config.getDatashieldDefaultBlacklist()).thenReturn(null);

    DatashieldContainerFactory factory = new DatashieldContainerFactory(config);
    DatashieldContainerConfig result = (DatashieldContainerConfig) factory.createDefault();

    assertEquals("default", result.getName());
    assertEquals("datashield/molgenis-rock-base:latest", result.getImage());
    assertEquals(Set.of("dsBase"), result.getPackageWhitelist());
    assertEquals(Set.of(), result.getFunctionBlacklist());
  }
}
