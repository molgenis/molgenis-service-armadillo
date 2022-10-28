package org.molgenis.armadillo.controller;

import static java.util.Collections.emptySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.AccessLoader;
import org.molgenis.armadillo.metadata.AccessMetadata;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.ProjectDetails;
import org.molgenis.armadillo.metadata.ProjectPermission;
import org.molgenis.armadillo.metadata.UserDetails;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class})
class AdminControllerTest {

  public static final String EXAMPLE_SETTINGS =
      "{\"users\": {\"bofke@email.com\": {\"email\": \"bofke@email.com\"}}, \"projects\": {\"bofkesProject\":{\"name\": \"bofkesProject\"}}, \"permissions\": [{\"email\":  \"bofke@email.com\", \"project\":\"bofkesProject\"}]}";
  @MockBean ArmadilloStorageService armadilloStorage;

  @Autowired AuditEventPublisher auditEventPublisher;
  @Autowired MockMvc mockMvc;
  @MockBean JwtDecoder jwtDecoder;
  @MockBean AccessLoader accessLoader;
  @Autowired AccessService accessService;
  @MockBean DockerService dockerService;

  @BeforeEach
  public void before() {
    var exampleSettings = createExampleSettings();
    when(accessLoader.load()).thenReturn(exampleSettings);
    runAsSystem(() -> accessService.initialize());
  }

  private AccessMetadata createExampleSettings() {
    var settings = AccessMetadata.create();
    settings.getUsers().put("bofke@email.com", UserDetails.create("bofke@email.com"));
    settings
        .getProjects()
        .put("bofkesProject", ProjectDetails.create("bofkesProject", Set.of("bofke@email.com")));
    settings.getPermissions().add(ProjectPermission.create("bofke@email.com", "bofkesProject"));
    return settings;
  }

  @Test
  @WithMockUser(roles = "SU")
  void settings_GET() throws Exception {
    mockMvc
        .perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(EXAMPLE_SETTINGS));
  }

  @Test
  @WithMockUser(roles = "SU")
  void permissions_POST() throws Exception {
    mockMvc
        .perform(
            post("/admin/permissions")
                .param("project", "chefkesProject")
                .param("email", "chefke@email.com")
                .with(csrf()))
        .andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected
        .getUsers()
        .put(
            "chefke@email.com",
            UserDetails.create("chefke@email.com", null, null, null, null, emptySet()));
    expected
        .getProjects()
        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));
    verify(accessLoader).save(expected);
  }

  @Test
  @WithMockUser(roles = "SU")
  void permissions_GET() throws Exception {
    mockMvc
        .perform(get("/admin/permissions"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content().json("[{\"email\": \"bofke@email.com\", \"project\": \"bofkesProject\"}]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void permissions_DELETE() throws Exception {
    mockMvc
        .perform(
            delete("/admin/permissions")
                .param("email", "bofke@email.com")
                .param("project", "bofkesProject")
                .with(csrf()))
        .andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected.getPermissions().clear();
    verify(accessLoader).save(expected);
  }

  @Test
  @WithMockUser(roles = "SU")
  void projects_GET() throws Exception {
    mockMvc
        .perform(get("/admin/projects"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content().json("[{\"name\":\"bofkesProject\", \"users\":[\"bofke@email.com\"]}]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  @WithUserDetails("bofke")
  void projects_name_GET() throws Exception {
    mockMvc
        .perform(get("/admin/projects/bofkesProject"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{\"name\":\"bofkesProject\"}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void projects_PUT() throws Exception {
    mockMvc
        .perform(
            put("/admin/projects")
                .content(
                    new Gson()
                        .toJson(
                            ProjectDetails.create("chefkesProject", Set.of("chefke@email.com"))))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isNoContent());

    var expected = createExampleSettings();
    expected.getUsers().put("chefke@email.com", UserDetails.create("chefke@email.com"));
    expected
        .getProjects()
        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));
    verify(accessLoader).save(expected);
  }

  @Test
  @WithMockUser(roles = "SU")
  void projects_DELETE() throws Exception {
    mockMvc
        .perform(delete("/admin/projects/bofkesProject").contentType(TEXT_PLAIN).with(csrf()))
        .andExpect(status().isNoContent());

    var expected = AccessMetadata.create();
    expected.getUsers().put("bofke@email.com", UserDetails.create("bofke@email.com"));
    verify(accessLoader).save(expected);
  }

  @Test
  @WithMockUser
  void settings_projects_GET_PermissionDenied() throws Exception {
    mockMvc.perform(get("/admin/projects")).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = "SU")
  void users_GET() throws Exception {
    mockMvc
        .perform(get("/admin/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[{\"email\":\"bofke@email.com\"}]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void users_GET_byEmail() throws Exception {
    mockMvc
        .perform(get("/admin/users/bofke@email.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{\"email\": \"bofke@email.com\"}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void users_PUT() throws Exception {
    var expected = createExampleSettings();
    expected
        .getUsers()
        .put(
            "chefke@email.com",
            UserDetails.create(
                "chefke@email.com", "Chefke", "von Chefke", "Chefke & co", true, emptySet()));
    expected
        .getProjects()
        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));

    when(accessLoader.save(expected)).thenReturn(expected);

    String testUser =
        new Gson()
            .toJson(
                UserDetails.create(
                    "chefke@email.com",
                    "Chefke",
                    "von Chefke",
                    "Chefke & co",
                    true,
                    Set.of("chefkesProject")));
    mockMvc
        .perform(put("/admin/users").content(testUser).contentType(APPLICATION_JSON).with(csrf()))
        .andExpect(status().isNoContent());

    verify(accessLoader).save(expected);

    // check that 'get' also in sync
    mockMvc
        .perform(get("/admin/users/chefke@email.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(testUser));
  }

  @Test
  @WithMockUser(roles = "SU")
  void users_email_DELETE() throws Exception {
    mockMvc
        .perform(delete("/admin/users/bofke@email.com").with(csrf()))
        .andExpect(status().isNoContent());

    var expected = AccessMetadata.create();
    expected
        .getProjects()
        .put("bofkesProject", ProjectDetails.create("bofkesProject", Set.of("bofke@email.com")));
    verify(accessLoader).save(expected);
  }
}
