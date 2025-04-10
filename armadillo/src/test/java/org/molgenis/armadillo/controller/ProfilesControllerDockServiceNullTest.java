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
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;

@WebMvcTest(ProfilesController.class)
@Import({TestSecurityConfig.class})
class ProfilesControllerDockServiceNullTest extends ArmadilloControllerTestBase {

  @Autowired ProfileService profileService;
  @MockBean ArmadilloStorageService armadilloStorage;
  @MockBean ProfilesLoader profilesLoader;

  @BeforeEach
  void before() {
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
