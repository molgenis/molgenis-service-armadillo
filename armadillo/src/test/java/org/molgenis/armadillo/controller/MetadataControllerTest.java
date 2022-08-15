package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.molgenis.armadillo.metadata.ProjectDetails;
import org.molgenis.armadillo.metadata.UserDetails;
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

@WebMvcTest(MetadataController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class})
class MetadataControllerTest {
  public static final String EXAMPLE_SETTINGS =
      "{\"users\": {\"bofke@email.com\": {\"email\": \"bofke@email.com\"}}, \"projects\": {\"bofkesProject\":{\"name\": \"bofkesProject\"}}, \"permissions\": [{\"email\": \"bofke@email.com\", \"project\":\"bofkesProject\"}]}";
  @MockBean private ArmadilloStorageService armadilloStorage;
  @Autowired AuditEventPublisher auditEventPublisher;
  @Autowired private MockMvc mockMvc;
  @MockBean JwtDecoder jwtDecoder;
  @Autowired ArmadilloMetadataService armadilloMetadataService;

  @BeforeEach
  void setup() {
    // default state
    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(new ByteArrayInputStream(EXAMPLE_SETTINGS.getBytes()));
    armadilloMetadataService.reload();
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
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            post("/admin/permissions")
                .param("project", "chefkesProject")
                .param("email", "chefke@email.com")
                .with(csrf()))
        .andExpect(status().isCreated());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\"},\"chefke@email.com\":{\"email\":\"chefke@email.com\"}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\"},\"bofkesProject\":{\"name\":\"bofkesProject\"}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}",
        new String(argument.getValue().readAllBytes()));
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
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            delete("/admin/permissions")
                .param("email", "bofke@email.com")
                .param("project", "bofkesProject")
                .with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\"}},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\"}},\"permissions\":[]}",
        new String(argument.getValue().readAllBytes()));
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
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            put("/admin/projects")
                .content(
                    new Gson()
                        .toJson(
                            ProjectDetails.create("chefkesProject", Set.of("chefke@email.com"))))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\"},\"chefke@email.com\":{\"email\":\"chefke@email.com\"}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\"},\"bofkesProject\":{\"name\":\"bofkesProject\"}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}",
        new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  void projects_DELETE() throws Exception {
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(delete("/admin/projects/bofkesProject").contentType(TEXT_PLAIN).with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\"}},\"projects\":{},\"permissions\":[]}",
        new String(argument.getValue().readAllBytes()));
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
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);

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
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    final String backendState =
        "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\"},\"chefke@email.com\":{\"email\":\"chefke@email.com\",\"firstName\":\"Chefke\",\"lastName\":\"von Chefke\",\"institution\":\"Chefke & co\",\"admin\":true}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\"},\"bofkesProject\":{\"name\":\"bofkesProject\"}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}";
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(backendState, new String(argument.getValue().readAllBytes()));

    // check that 'get' also in sync
    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(new ByteArrayInputStream(backendState.getBytes()));
    armadilloMetadataService.reload();
    mockMvc
        .perform(get("/admin/users/chefke@email.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(testUser));
  }

  @Test
  @WithMockUser(roles = "SU")
  void users_email_DELETE() throws Exception {
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc.perform(delete("/admin/users/bofke@email.com").with(csrf())).andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\"}},\"permissions\":[]}",
        new String(argument.getValue().readAllBytes()));
  }
}
