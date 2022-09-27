// package org.molgenis.armadillo.controller;
//
// import static java.util.Collections.emptySet;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.mockito.Mockito.eq;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;
// import static org.molgenis.armadillo.metadata.ProfileStatus.RUNNING;
// import static org.molgenis.armadillo.security.RunAs.runAsSystem;
// import static org.springframework.http.MediaType.APPLICATION_JSON;
// import static org.springframework.http.MediaType.TEXT_PLAIN;
// import static
// org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
// import com.google.gson.Gson;
// import java.io.ByteArrayInputStream;
// import java.util.Map;
// import java.util.Set;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.molgenis.armadillo.audit.AuditEventPublisher;
// import org.molgenis.armadillo.metadata.ArmadilloMetadata;
// import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
// import org.molgenis.armadillo.metadata.MetadataLoader;
// import org.molgenis.armadillo.metadata.ProfileConfig;
// import org.molgenis.armadillo.metadata.ProjectDetails;
// import org.molgenis.armadillo.metadata.ProjectPermission;
// import org.molgenis.armadillo.metadata.UserDetails;
// import org.molgenis.armadillo.profile.ProfileService;
// import org.molgenis.armadillo.storage.ArmadilloStorageService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.Import;
// import org.springframework.security.oauth2.jwt.JwtDecoder;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.security.test.context.support.WithUserDetails;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;
//
// @WebMvcTest(AdminController.class)
// @ExtendWith(MockitoExtension.class)
// @ActiveProfiles("test")
// @Import({AuditEventPublisher.class})
// class AdminControllerTest {
//  public static final String DEFAULT_PROFILE =
//
// "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}";
//  public static final String EXAMPLE_SETTINGS =
//      "{\"users\": {\"bofke@email.com\": {\"email\": \"bofke@email.com\"}}, \"projects\":
// {\"bofkesProject\":{\"name\": \"bofkesProject\"}}, \"permissions\": [{\"email\":
// \"bofke@email.com\", \"project\":\"bofkesProject\"}],\"profiles\":{\"default\":"
//          + DEFAULT_PROFILE
//          + "}}";
//  @MockBean ArmadilloStorageService armadilloStorage;
//
//  @Autowired AuditEventPublisher auditEventPublisher;
//  @Autowired MockMvc mockMvc;
//  @MockBean JwtDecoder jwtDecoder;
//  @MockBean MetadataLoader metadataLoader;
//  @Autowired ArmadilloMetadataService armadilloMetadataService;
//  @MockBean ProfileService armadilloProfileService;
//
//  @BeforeEach
//  public void before() {
//    var exampleSettings = createExampleSettings();
//    when(metadataLoader.load()).thenReturn(exampleSettings);
//    runAsSystem(() -> armadilloMetadataService.initialize());
//
//    when(armadilloProfileService.getProfileStatus(
//        ProfileConfig.create(
//            "default",
//            "datashield/armadillo-rserver:6.2.0",
//            "localhost",
//            6311,
//            Set.of("dsBase"),
//            Map.of(),
//            null)))
//        .thenReturn(RUNNING);
//  }
//
//  private ArmadilloMetadata createExampleSettings() {
//    var settings = ArmadilloMetadata.create();
//    settings.getUsers().put("bofke@email.com", UserDetails.create("bofke@email.com"));
//    settings
//        .getProjects()
//        .put("bofkesProject", ProjectDetails.create("bofkesProject", Set.of("bofke@email.com")));
//    settings.getPermissions().add(ProjectPermission.create("bofke@email.com", "bofkesProject"));
//    return settings;
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void settings_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(content().json(EXAMPLE_SETTINGS));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void permissions_POST() throws Exception {
//    mockMvc
//        .perform(
//            post("/admin/permissions")
//                .param("project", "chefkesProject")
//                .param("email", "chefke@email.com")
//                .with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]},\"chefke@email.com\":{\"email\":\"chefke@email.com\",\"projects\":[]}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\",\"users\":[]},\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}",
//        new String(argument.getValue().readAllBytes()));
// =======
//    var expected = createExampleSettings();
//    expected
//        .getUsers()
//        .put(
//            "chefke@email.com",
//            UserDetails.create("chefke@email.com", null, null, null, null, emptySet()));
//    expected
//        .getProjects()
//        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
//    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void permissions_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/permissions"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(
//            content().json("[{\"email\": \"bofke@email.com\", \"project\": \"bofkesProject\"}]"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void permissions_DELETE() throws Exception {
//    mockMvc
//        .perform(
//            delete("/admin/permissions")
//                .param("email", "bofke@email.com")
//                .param("project", "bofkesProject")
//                .with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]}},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[]}",
//        new String(argument.getValue().readAllBytes()));
// =======
//    var expected = createExampleSettings();
//    expected.getPermissions().clear();
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void projects_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/projects"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(
//            content().json("[{\"name\":\"bofkesProject\", \"users\":[\"bofke@email.com\"]}]"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  @WithUserDetails("bofke")
//  void projects_name_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/projects/bofkesProject"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(content().json("{\"name\":\"bofkesProject\"}"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void projects_PUT() throws Exception {
//    mockMvc
//        .perform(
//            put("/admin/projects")
//                .content(
//                    new Gson()
//                        .toJson(
//                            ProjectDetails.create("chefkesProject", Set.of("chefke@email.com"))))
//                .contentType(APPLICATION_JSON)
//                .with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]},\"chefke@email.com\":{\"email\":\"chefke@email.com\",\"projects\":[]}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\",\"users\":[]},\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}",
//        new String(argument.getValue().readAllBytes()));
// =======
//    var expected = createExampleSettings();
//    expected.getUsers().put("chefke@email.com", UserDetails.create("chefke@email.com"));
//    expected
//        .getProjects()
//        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
//    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void projects_DELETE() throws Exception {
//    mockMvc
//        .perform(delete("/admin/projects/bofkesProject").contentType(TEXT_PLAIN).with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]}},\"projects\":{},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[]}",
//        new String(argument.getValue().readAllBytes()));
// =======
//    var expected = ArmadilloMetadata.create();
//    expected.getUsers().put("bofke@email.com", UserDetails.create("bofke@email.com"));
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  }
//
//  @Test
//  @WithMockUser
//  void settings_projects_GET_PermissionDenied() throws Exception {
//    mockMvc.perform(get("/admin/projects")).andExpect(status().is(403));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void users_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/users"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(content().json("[{\"email\":\"bofke@email.com\"}]"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void users_GET_byEmail() throws Exception {
//    mockMvc
//        .perform(get("/admin/users/bofke@email.com"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(content().json("{\"email\": \"bofke@email.com\"}"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void users_PUT() throws Exception {
//    var expected = createExampleSettings();
//    expected
//        .getUsers()
//        .put(
//            "chefke@email.com",
//            UserDetails.create(
//                "chefke@email.com", "Chefke", "von Chefke", "Chefke & co", true, emptySet()));
//    expected
//        .getProjects()
//        .put("chefkesProject", ProjectDetails.create("chefkesProject", emptySet()));
//    expected.getPermissions().add(ProjectPermission.create("chefke@email.com", "chefkesProject"));
//
//    when(metadataLoader.save(expected)).thenReturn(expected);
//
//    String testUser =
//        new Gson()
//            .toJson(
//                UserDetails.create(
//                    "chefke@email.com",
//                    "Chefke",
//                    "von Chefke",
//                    "Chefke & co",
//                    true,
//                    Set.of("chefkesProject")));
//    mockMvc
//        .perform(put("/admin/users").content(testUser).contentType(APPLICATION_JSON).with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    final String backendState =
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]},\"chefke@email.com\":{\"email\":\"chefke@email.com\",\"firstName\":\"Chefke\",\"lastName\":\"von Chefke\",\"institution\":\"Chefke & co\",\"admin\":true,\"projects\":[]}},\"projects\":{\"chefkesProject\":{\"name\":\"chefkesProject\",\"users\":[]},\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"},{\"email\":\"chefke@email.com\",\"project\":\"chefkesProject\"}]}";
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(backendState, new String(argument.getValue().readAllBytes()));
// =======
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//
//    // check that 'get' also in sync
//    mockMvc
//        .perform(get("/admin/users/chefke@email.com"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(content().json(testUser));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void users_email_DELETE() throws Exception {
//    mockMvc
//        .perform(delete("/admin/users/bofke@email.com").with(csrf()))
//        .andExpect(status().isNoContent());
//
// <<<<<<< HEAD
//    // verify mock magic, I must say I prefer integration tests above this nonsense
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[]}",
//        new String(argument.getValue().readAllBytes()));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void profiles_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/profiles"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(
//            content()
//                .json(
//
// "[{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"]}]"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void profiles_name_GET() throws Exception {
//    mockMvc
//        .perform(get("/admin/profiles/default"))
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(
//            content()
//                .json(
//
// "{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"]}"));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void profiles_PUT() throws Exception {
//    ProfileConfig profileConfig =
//        ProfileConfig.create(
//            "dummy", "dummy/armadillo:2.0.0", "localhost", 6312, Set.of("dsBase"), Map.of(),
// null);
//
//    ArgumentCaptor<ByteArrayInputStream> argument =
//        ArgumentCaptor.forClass(ByteArrayInputStream.class);
//    mockMvc
//        .perform(
//            put("/admin/profiles")
//                .content(new Gson().toJson(profileConfig))
//                .contentType(APPLICATION_JSON)
//                .with(csrf()))
//        .andExpect(status().isOk());
//
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]}},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{\"dummy\":{\"name\":\"dummy\",\"image\":\"dummy/armadillo:2.0.0\",\"port\":6312,\"whitelist\":[\"dsBase\"],\"options\":{}},\"default\":{\"name\":\"default\",\"image\":\"datashield/armadillo-rserver:6.2.0\",\"port\":6311,\"whitelist\":[\"dsBase\"],\"options\":{}}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"}]}",
//        new String(argument.getValue().readAllBytes()));
//  }
//
//  @Test
//  @WithMockUser(roles = "SU")
//  void profiles_DELETE() throws Exception {
//    ArgumentCaptor<ByteArrayInputStream> argument =
//        ArgumentCaptor.forClass(ByteArrayInputStream.class);
//    mockMvc
//        .perform(delete("/admin/profiles/default").contentType(TEXT_PLAIN).with(csrf()))
//        .andExpect(status().isNoContent());
//
//    verify(armadilloStorage)
//        .saveSystemFile(argument.capture(), eq(METADATA_FILE), eq(APPLICATION_JSON));
//    assertEquals(
//
// "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",\"projects\":[]}},\"projects\":{\"bofkesProject\":{\"name\":\"bofkesProject\",\"users\":[]}},\"profiles\":{},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"bofkesProject\"}]}",
//        new String(argument.getValue().readAllBytes()));
// =======
//    var expected = ArmadilloMetadata.create();
//    expected
//        .getProjects()
//        .put("bofkesProject", ProjectDetails.create("bofkesProject", Set.of("bofke@email.com")));
//    verify(metadataLoader).save(expected);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  }
// }
