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

import java.util.List;
import java.util.Map;
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
      "{\"type\":\"ds\",\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,"
          + "\"specificContainerData\":{\"packageWhitelist\":[\"dsBase\"],\"options\":{}}}";

  public static final String OMICS_CONTAINER =
      "{\"type\":\"ds\",\"name\":\"omics\",\"image\":\"datashield/armadillo-rserver-omics\",\"port\":6312,"
          + "\"specificContainerData\":{\"packageWhitelist\":[\"dsBase\", \"dsOmics\"],\"options\":{}}}";

  @Autowired ContainerService containerService;
  @MockitoBean ArmadilloStorageService armadilloStorage;
  @MockitoBean DockerService dockerService;
  @MockitoBean ContainersLoader containersLoader;
  @MockitoBean ContainerScheduler containerScheduler;

  @BeforeEach
  public void before() {
    reset(dockerService, containersLoader, containerScheduler);

    when(dockerService.getContainerStatus(anyString()))
        .thenReturn(ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE));
    when(dockerService.getContainerEnvironmentConfig(anyString())).thenReturn(new String[0]);

    var settings = createExampleSettings();
    when(containersLoader.load()).thenReturn(settings);
    when(containersLoader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    runAsSystem(() -> containerService.initialize());
  }

  private ContainersMetadata createExampleSettings() {
    var settings = ContainersMetadata.create();
    settings
        .getContainers()
        .put(
            "datashield-default",
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
                emptyMap(),
                List.of(),
                Map.of()));
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
                emptyMap(),
                List.of(),
                Map.of()));
    settings
        .getContainers()
        .put(
            "non-datashield-default",
            DefaultContainerConfig.create(
                "default-other",
                "other/image:1.0.0",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of()));
    return settings;
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_GET() throws Exception {
    mockMvc
        .perform(get("/containers"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "["
                        + "{\"name\":\"omics\"},"
                        + "{\"name\":\"default-other\"},"
                        + "{\"name\":\"DEFAULT\"},"
                        + "{\"name\":\"default\"}"
                        + "]",
                    false));
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_name_GET_default() throws Exception {
    mockMvc
        .perform(get("/containers/non-datashield-default"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
            {
              "type": "default",
              "name": "default-other",
              "image": "other/image:1.0.0",
              "port": 6311
            }
            """,
                    false)); // Lenient: ignores specificContainerData if empty/null
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_name_GET_datashield() throws Exception {
    mockMvc
        .perform(get("/containers/datashield-default"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
          {
            "type": "ds",
            "name": "default",
            "image": "datashield/armadillo-rserver:6.2.0",
            "port": 6311,
            "specificContainerOptions": {
              "packageWhitelist": ["dsBase"]
            }
          }
          """,
                    false));
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_PUT() throws Exception {
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
    clearInvocations(containersLoader);
    mockMvc.perform(delete("/containers/default")).andExpect(status().isConflict());

    verify(containersLoader, never()).save(any());
  }

  @Test
  @WithMockUser(roles = "SU")
  void containers_DELETE() throws Exception {
    clearInvocations(containersLoader);
    mockMvc.perform(delete("/containers/omics")).andExpect(status().isNoContent());
    verify(containersLoader).save(any());
  }

  @Test
  @WithAnonymousUser
  void getContainerStatus_GET() throws Exception {
    ContainerInfo runningContainer = ContainerInfo.create(ContainerStatus.RUNNING);
    ContainerInfo offlineContainer = ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE);

    when(dockerService.getContainerStatus("default")).thenReturn(runningContainer);
    when(dockerService.getContainerStatus("omics")).thenReturn(offlineContainer);
    when(dockerService.getContainerStatus("default-other")).thenReturn(offlineContainer);
    when(dockerService.getContainerStatus("DEFAULT")).thenReturn(offlineContainer);

    String[] config = {
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
    when(dockerService.getContainerEnvironmentConfig(anyString())).thenReturn(new String[0]);

    mockMvc
        .perform(get("/containers/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "["
                        + "{\"name\":\"default\",\"status\":\"RUNNING\"},"
                        + "{\"name\":\"omics\",\"status\":\"DOCKER_OFFLINE\"},"
                        + "{\"name\":\"default-other\",\"status\":\"DOCKER_OFFLINE\"},"
                        + "{\"name\":\"DEFAULT\",\"status\":\"DOCKER_OFFLINE\"}"
                        + "]",
                    false)); // 'false' ignores extra fields and strict ordering
  }
}
