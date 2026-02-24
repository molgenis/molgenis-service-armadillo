package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.container.ContainerInfo;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.DefaultContainerConfig;
import org.molgenis.armadillo.controller.ContainerResponse.DatashieldResponse;
import org.molgenis.armadillo.controller.ContainerResponse.DefaultResponse;
import org.molgenis.armadillo.metadata.ContainerStatus;

class ContainerResponseTest {

  @Test
  void create_forDefaultContainer() {
    var config =
        DefaultContainerConfig.create(
            "default",
            "image",
            "localhost",
            6311,
            123L,
            "2024-01-01",
            "img1",
            java.util.List.of("--arg"),
            Map.of("opt", "value"));
    var info = ContainerInfo.create(ContainerStatus.RUNNING);

    ContainerResponse response = ContainerResponse.create(config, info);

    assertTrue(response instanceof DefaultResponse);
    var defaultResponse = (DefaultResponse) response;
    assertEquals("default", defaultResponse.type());
    assertEquals("default", defaultResponse.name());
    assertEquals("image", defaultResponse.image());
    assertEquals(6311, defaultResponse.port());
    assertEquals(123L, defaultResponse.imageSize());
    assertEquals("2024-01-01", defaultResponse.installDate());
    assertEquals("img1", defaultResponse.lastImageId());
    assertEquals(java.util.List.of("--arg"), defaultResponse.dockerArgs());
    assertEquals(Map.of("opt", "value"), defaultResponse.dockerOptions());
    assertEquals(info, defaultResponse.containerInfo());
  }

  @Test
  void create_forDatashieldContainer() {
    var config =
        DatashieldContainerConfig.builder()
            .name("ds")
            .image("image")
            .host("localhost")
            .port(6311)
            .packageWhitelist(Set.of("dsBase"))
            .functionBlacklist(Set.of())
            .datashieldROptions(Map.of("datashield.seed", "123456789"))
            .build();
    var info = ContainerInfo.create(ContainerStatus.NOT_RUNNING);

    ContainerResponse response = ContainerResponse.create(config, info);

    assertTrue(response instanceof DatashieldResponse);
    var dsResponse = (DatashieldResponse) response;
    assertEquals("ds", dsResponse.type());
    assertEquals("ds", dsResponse.name());
    assertEquals("image", dsResponse.image());
    assertEquals(6311, dsResponse.port());
    assertEquals(info, dsResponse.containerInfo());
    assertEquals(
        Map.of(
            "packageWhitelist",
            Set.of("dsBase"),
            "functionBlacklist",
            Set.of(),
            "datashieldROptions",
            Map.of("datashield.seed", "123456789")),
        dsResponse.specificContainerOptions());
  }
}
