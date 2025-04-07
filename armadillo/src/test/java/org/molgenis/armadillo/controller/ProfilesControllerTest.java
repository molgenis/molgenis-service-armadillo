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

import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.profile.ContainerInfo;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(ProfilesController.class)
@Import({TestSecurityConfig.class})
class ProfilesControllerTest extends ArmadilloControllerTestBase {

  public static final String DEFAULT_PROFILE =
      "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"packageWhitelist\":[\"dsBase\"],\"options\":{}}";
  public static final String OMICS_PROFILE =
      "{\"name\":\"omics\",\"image\":\"datashield/armadillo-rserver-omics\",\"port\":6312,\"packageWhitelist\":[\"dsBase\", \"dsOmics\"],\"options\":{}}";

  @Autowired ProfileService profileService;
  @MockBean ArmadilloStorageService armadilloStorage;
  @MockBean DockerService dockerService;
  @MockBean ProfilesLoader profilesLoader;

  @BeforeEach
  public void before() {
    var settings = createExampleSettings();
    when(profilesLoader.load()).thenReturn(settings);
    runAsSystem(() -> profileService.initialize());
  }

  private ProfilesMetadata createExampleSettings() {
    var settings = ProfilesMetadata.create();
    settings
        .getProfiles()
        .put(
            "default",
            ProfileConfig.create(
                "default",
                "datashield/armadillo-rserver:6.2.0",
                "localhost",
                6311,
                Set.of("dsBase"),
                emptySet(),
                emptyMap()));
    settings
        .getProfiles()
        .put(
            "omics",
            ProfileConfig.create(
                "omics",
                "datashield/armadillo-rserver-omics",
                "localhost",
                6312,
                Set.of("dsBase", "dsOmics"),
                emptySet(),
                emptyMap()));
    return settings;
  }

  @Test
  @WithMockUser(roles = "SU")
  void profiles_GET() throws Exception {
    mockMvc
        .perform(get("/ds-profiles"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[" + DEFAULT_PROFILE + "," + OMICS_PROFILE + "]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void profiles_name_GET() throws Exception {
    mockMvc
        .perform(get("/ds-profiles/default"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"packageWhitelist\":[\"dsBase\"]}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void profiles_PUT() throws Exception {
    ProfileConfig profileConfig =
        ProfileConfig.create(
            "dummy",
            "dummy/armadillo:2.0.0",
            "localhost",
            6312,
            Set.of("dsBase"),
            emptySet(),
            Map.of());

    mockMvc
        .perform(
            put("/ds-profiles")
                .content(new Gson().toJson(profileConfig))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected.getProfiles().put("dummy", profileConfig);
    verify(profilesLoader).save(expected);
  }

  @Test
  @WithMockUser(roles = "SU")
  void profiles_DELETE_default() throws Exception {
    mockMvc.perform(delete("/ds-profiles/default")).andExpect(status().isConflict());

    verify(profilesLoader, never()).save(any());
  }

  @Test
  @WithMockUser(roles = "SU")
  void profiles_DELETE() throws Exception {
    mockMvc.perform(delete("/ds-profiles/omics")).andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected.getProfiles().remove("omics");
    verify(profilesLoader).save(expected);
  }

  @Test
  @WithAnonymousUser
  void getProfileStatus_GET() throws Exception {
    // DockerService dockerService = mock(DockerService.class);
    ContainerInfo runningContainer = ContainerInfo.create(ProfileStatus.RUNNING);
    ContainerInfo offlineContainer = ContainerInfo.create(ProfileStatus.DOCKER_OFFLINE);
    // Mock DockerService to return a specific status
    when(dockerService.getProfileStatus("default"))
        .thenReturn(runningContainer); // Example of a running container
    when(dockerService.getProfileStatus("omics"))
        .thenReturn(offlineContainer); // Example of a stopped container
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

    when(dockerService.getProfileEnvironmentConfig("default")).thenReturn(config);

    // Perform the GET request to fetch the profile status
    mockMvc
        .perform(get("/ds-profiles/status"))
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
