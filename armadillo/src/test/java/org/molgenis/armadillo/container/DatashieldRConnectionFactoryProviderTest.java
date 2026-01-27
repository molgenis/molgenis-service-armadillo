package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnectionFactory;

class DatashieldRConnectionFactoryProviderTest {

  @Test
  void create_returnsRServerConnectionFactory() {
    DatashieldContainerConfig config =
        DatashieldContainerConfig.builder()
            .name("default")
            .host("localhost")
            .port(6311)
            .packageWhitelist(Set.of())
            .functionBlacklist(Set.of())
            .build();

    DatashieldRConnectionFactoryProvider provider = new DatashieldRConnectionFactoryProvider();
    RConnectionFactory factory = provider.create(config);

    assertNotNull(factory);
    assertEquals(RServerConnectionFactory.class, factory.getClass());
  }
}
