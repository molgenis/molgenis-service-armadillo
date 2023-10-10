package org.molgenis.armadillo.controller;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfilesLoader;
import org.molgenis.armadillo.metadata.ProfilesMetadata;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
                false,
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
                false,
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
            false,
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
}
