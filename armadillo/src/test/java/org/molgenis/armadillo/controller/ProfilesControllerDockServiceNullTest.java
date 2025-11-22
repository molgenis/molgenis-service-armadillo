package org.molgenis.armadillo.controller;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.profile.ProfileScheduler;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ProfilesController.class)
@Import({TestSecurityConfig.class})
class ProfilesControllerDockServiceNullTest extends ArmadilloControllerTestBase {

  @Autowired ContainerService containerService;
  @MockitoBean ArmadilloStorageService armadilloStorage;
  @MockitoBean ContainersLoader containersLoader;
  @MockitoBean ProfileScheduler profileScheduler;

  @BeforeEach
  void before() {
    var settings = createExampleSettings();
    when(containersLoader.load()).thenReturn(settings);
    runAsSystem(() -> containerService.initialize());
  }

  private ContainersMetadata createExampleSettings() {
    var settings = ContainersMetadata.create();
    settings
        .getProfiles()
        .put(
            "default",
            ContainerConfig.create(
                "default",
                "datashield/armadillo-rserver:6.2.0",
                false,
                null,
                "localhost",
                6311,
                Set.of("dsBase"),
                emptySet(),
                emptyMap(),
                null,
                null,
                null,
                null,
                null));
    settings
        .getProfiles()
        .put(
            "omics",
            ContainerConfig.create(
                "omics",
                "datashield/armadillo-rserver-omics",
                false,
                null,
                "localhost",
                6312,
                Set.of("dsBase", "dsOmics"),
                emptySet(),
                emptyMap(),
                null,
                null,
                null,
                null,
                null));
    return settings;
  }

  @Test
  @WithAnonymousUser
  void getProfileStatusWithoutDocker_GET() throws Exception {
    mockMvc
        .perform(get("/ds-profiles/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "["
                        + "{\"name\":\"default\",\"status\":\"DOCKER_MANAGEMENT_DISABLED\",\"config\":\"[]\",\"image\":\"datashield/armadillo-rserver:6.2.0\"},"
                        + "{\"name\":\"omics\",\"status\":\"DOCKER_MANAGEMENT_DISABLED\",\"config\":\"[]\",\"image\":\"datashield/armadillo-rserver-omics\"}"
                        + "]"));
  }
}
