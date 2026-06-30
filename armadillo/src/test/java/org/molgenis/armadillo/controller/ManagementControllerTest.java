package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.gson.Gson;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.security.AuthConfig;
import org.molgenis.armadillo.security.LoginAttemptTracker;
import org.molgenis.armadillo.security.NoPopupBasicAuthenticationEntryPoint;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// false positive all methods have testcases, but mockmvc isn't picked up properly
@java.lang.SuppressWarnings({"java:S2699"})
@WebMvcTest(ManagementController.class)
@Import(AuthConfig.class)
@TestPropertySource(
    properties = {"spring.security.user.name=admin", "spring.security.user.password=password"})
class ManagementControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
  @MockitoBean ManagementService managementService;
  @MockitoBean AuditEventPublisher auditor;
  @MockitoBean org.molgenis.armadillo.metadata.AccessService accessService;
  @MockitoBean ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean LoginAttemptTracker loginAttemptTracker;

  @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
  NoPopupBasicAuthenticationEntryPoint noPopupBasicAuthenticationEntryPoint;

  @Test
  @WithMockUser(roles = "SU")
  void softRestart_POST() throws Exception {
    mockMvc.perform(post("/manage/app/restart/soft").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("TRIGGER_SOFT_RESTART"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void softRestart_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void softRestart_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void hardRestart_POST() throws Exception {
    mockMvc.perform(post("/manage/app/restart/hard").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("TRIGGER_HARD_RESTART"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void hardRestart_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/hard").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void hardRestart_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/hard").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void update_POST() throws Exception {
    mockMvc
        .perform(post("/manage/app/update").param("version", "1.2.3").with(csrf()))
        .andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("UPDATE_ARMADILLO"), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  void update_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/update").param("version", "1.2.3").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/update").param("version", "1.2.3").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void listAvailable_GET() throws Exception {
    when(auditor.audit(any(Supplier.class), any(Principal.class), eq("LIST_AVAILABLE_VERSIONS")))
        .thenReturn(Set.of("armadillo-1.0.0.jar", "armadillo-1.1.0.jar"));

    mockMvc.perform(get("/manage/app/list").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Supplier.class), any(Principal.class), eq("LIST_AVAILABLE_VERSIONS"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void listAvailable_GET_forbidden_for_non_su() throws Exception {
    mockMvc.perform(get("/manage/app/list").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void listAvailable_GET_unauthenticated() throws Exception {
    mockMvc.perform(get("/manage/app/list").with(csrf())).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void deleteJar_DELETE() throws Exception {
    mockMvc
        .perform(delete("/manage/app/delete-jar").param("version", "1.0.0").with(csrf()))
        .andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("DELETE_JAR"), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  void deleteJar_DELETE_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(delete("/manage/app/delete-jar").param("version", "1.0.0").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteJar_DELETE_unauthenticated() throws Exception {
    mockMvc
        .perform(delete("/manage/app/delete-jar").param("version", "1.0.0").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void getLastReleaseInfo_GET() throws Exception {
    when(auditor.audit(any(Supplier.class), any(Principal.class), eq("GET_RELEASE_VERSION")))
        .thenReturn(Map.of("tag_name", "v1.2.3"));

    mockMvc.perform(get("/manage/app/latest-release-info").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Supplier.class), any(Principal.class), eq("GET_RELEASE_VERSION"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getLastReleaseInfo_GET_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(get("/manage/app/latest-release-info").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getLastReleaseInfo_GET_unauthenticated() throws Exception {
    mockMvc
        .perform(get("/manage/app/latest-release-info").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void downloadUpdateScript_POST() throws Exception {
    mockMvc
        .perform(post("/manage/updater/download").param("armadilloVersion", "1.2.3").with(csrf()))
        .andExpect(status().isOk());

    verify(auditor)
        .audit(any(Supplier.class), any(Principal.class), eq("DOWNLOAD_UPDATE_SCRIPT"), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  void downloadUpdateScript_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/updater/download").param("armadilloVersion", "1.2.3").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void downloadUpdateScript_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/updater/download").param("armadilloVersion", "1.2.3").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void getOidcConfig_GET() throws Exception {
    Map<String, String> oidcConfig = new HashMap<>();
    oidcConfig.put("issuerUri", "http://auth-server");
    oidcConfig.put("clientId", "client-id");
    oidcConfig.put("clientSecret", "this-is-very-secret");
    oidcConfig.put("deviceClientId", "device-client-id");
    oidcConfig.put("deviceIssuerUri", "http://auth-server");
    when(managementService.getCurrentOidcConfig()).thenReturn(oidcConfig);

    mockMvc.perform(get("/manage/auth/oidc-config").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Supplier.class), any(Principal.class), eq("GET_OIDC_CONFIG"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOidcConfig_GET_forbidden_for_non_su() throws Exception {
    mockMvc.perform(get("/manage/auth/oidc-config").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void getOidcConfig_GET_unauthenticated() throws Exception {
    mockMvc
        .perform(get("/manage/auth/oidc-config").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "SU")
  void projectsUpsert_PUT() throws Exception {
    var oidcDetails =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(auditor)
        .audit(any(Runnable.class), any(Principal.class), eq("UPDATE_OIDC_CONFIG"), any());
  }

  @Test
  @WithMockUser(roles = "SU")
  void projectsUpsert_PUT_empty_body_returns_no_content() throws Exception {
    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content("{}")
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "USER")
  void projectsUpsert_PUT_forbidden_for_non_su() throws Exception {
    var oidcDetails =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void projectsUpsert_PUT_unauthenticated() throws Exception {
    var oidcDetails =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(
            result -> {
              result.equals("{\"attemptsRemaining\": 4}");
            });
  }
}
