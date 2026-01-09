package org.molgenis.armadillo.controller;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.container.*;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ContainersController.class)
@Import({TestSecurityConfig.class})
class ContainersControllerTest extends ArmadilloControllerTestBase {

  public static final String DEFAULT_CONTAINER =
      "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,"
          + "\"specificContainerData\":{\"packageWhitelist\":[\"dsBase\"],\"options\":{}}}";

  public static final String OMICS_CONTAINER =
      "{\"name\":\"omics\",\"image\":\"datashield/armadillo-rserver-omics\",\"port\":6312,"
          + "\"specificContainerData\":{\"packageWhitelist\":[\"dsBase\", \"dsOmics\"],\"options\":{}}}";

  @Autowired ContainerService containerService;
  @MockitoBean ArmadilloStorageService armadilloStorage;
  @MockitoBean DockerService dockerService;
  @MockitoBean ContainersLoader containersLoader;
  @MockitoBean ContainerScheduler containerScheduler;

  @BeforeEach
  public void before() {
    var settings = createExampleSettings();
    when(containersLoader.load()).thenReturn(settings);
    runAsSystem(() -> containerService.initialize());
  }

  private ContainersMetadata createExampleSettings() {
    var settings = ContainersMetadata.create();
    settings
        .getContainers()
        .put(
            "default",
            DatashieldContainerConfig.create(
                "default",
                "datashield/armadillo-rserver:6.2.0",
                "localhost",
                6311,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                Set.of("dsBase"),
                emptySet(),
                emptyMap()));
    settings
        .getContainers()
        .put(
            "omics",
            DatashieldContainerConfig.create(
                "omics",
                "datashield/armadillo-rserver-omics",
                "localhost",
                6312,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                Set.of("dsBase", "dsOmics"),
                emptySet(),
                emptyMap()));
    return settings;
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_GET() throws Exception {
    mockMvc
        .perform(get("/containers"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[" + DEFAULT_CONTAINER + "," + OMICS_CONTAINER + "]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_name_GET() throws Exception {
    mockMvc
        .perform(get("/containers/default"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"packageWhitelist\":[\"dsBase\"]}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_PUT() throws Exception {
    // Add "type":"ds" to the JSON string
    String json =
        "{"
            + "\"type\":\"ds\","
            + // THIS IS THE MISSING KEY
            "\"name\":\"dummy\","
            + "\"image\":\"dummy/armadillo:2.0.0\","
            + "\"host\":\"localhost\","
            + "\"port\":6312,"
            + "\"autoUpdate\":false,"
            + "\"packageWhitelist\":[\"dsBase\"],"
            + "\"functionBlacklist\":[],"
            + "\"options\":{}"
            + "}";

    mockMvc
        .perform(put("/containers").content(json).contentType(APPLICATION_JSON).with(csrf()))
        .andExpect(status().isNoContent()); // Should now return 204
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_DELETE_default() throws Exception {
    mockMvc.perform(delete("/containers/default")).andExpect(status().isConflict());

    verify(containersLoader, never()).save(any());
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_DELETE() throws Exception {
    mockMvc.perform(delete("/containers/omics")).andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected.getContainers().remove("omics");
    verify(containersLoader).save(expected);
  }

  @Test
  @WithAnonymousUser
  void getContainerStatus_GET() throws Exception {
    ContainerInfo runningContainer = ContainerInfo.create(ContainerStatus.RUNNING);
    ContainerInfo offlineContainer = ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE);
    when(dockerService.getContainerStatus("default")).thenReturn(runningContainer);
    when(dockerService.getContainerStatus("omics")).thenReturn(offlineContainer);
    String[] config = {
      "DEBUG=FALSE",
      "PATH=/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      "JAVA_HOME=/opt/java/openjdk",
      "LANG=C.UTF-8",
      "LANGUAGE=C.UTF-8",
      "LC_ALL=C.UTF-8",
      "JAVA_VERSION=jdk-21.0.5+11",
      "ROCK_VERSION=2.1.1",
      "DSBASE_VERSION=6.3.1",
      "DSMEDIATION_VERSION=0.0.3",
      "DSEXPOSOME_VERSION=2.0.9",
      "DSTIDYVERSE_VERSION=v1.0.1",
      "DSOMICS_VERSION=v1.0.18-2",
      "DSMTL_VERSION=0.9.9",
      "DSSURVIVAL_VERSION=v2.1.3"
    };

    when(dockerService.getContainerEnvironmentConfig("default")).thenReturn(config);

    mockMvc
        .perform(get("/containers/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "["
                        + "{\"name\":\"default\",\"status\":\"RUNNING\",\"config\":\"[JAVA_VERSION=jdk-21.0.5+11, ROCK_VERSION=2.1.1, DSBASE_VERSION=6.3.1, DSMEDIATION_VERSION=0.0.3, DSEXPOSOME_VERSION=2.0.9, DSTIDYVERSE_VERSION=v1.0.1, DSOMICS_VERSION=v1.0.18-2, DSMTL_VERSION=0.9.9, DSSURVIVAL_VERSION=v2.1.3]\",\"image\":\"datashield/armadillo-rserver:6.2.0\"},"
                        + "{\"name\":\"omics\",\"status\":\"DOCKER_OFFLINE\",\"config\":\"[]\",\"image\":\"datashield/armadillo-rserver-omics\"}"
                        + "]"));
  }
}
