package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.gson.Gson;
import java.security.Principal;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.security.AuthConfig;
import org.molgenis.armadillo.security.OidcConfig;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

  // ── GET /manage/auth/oidc-config ──────────────────────────────────────────

  @Test
  @WithMockUser(roles = "SU")
  void getOidcConfig_GET() throws Exception {
    var oidcConfig = mock(OidcConfig.class);
    when(auditor.audit(any(Supplier.class), any(Principal.class), eq("GET_OIDC_CONFIG")))
        .thenReturn(oidcConfig);

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

  // ── POST /manage/app/restart ──────────────────────────────────────────────

  @Test
  @WithMockUser(roles = "SU")
  void restart_POST() throws Exception {
    mockMvc.perform(post("/manage/app/restart").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("TRIGGER_RESTART"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void restart_POST_forbidden_for_non_su() throws Exception {
    mockMvc.perform(post("/manage/app/restart").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void restart_POST_unauthenticated() throws Exception {
    mockMvc.perform(post("/manage/app/restart").with(csrf())).andExpect(status().isUnauthorized());
  }

  // ── POST /manage/auth/reload ──────────────────────────────────────────────

  @Test
  @WithMockUser(roles = "SU")
  void reloadAuth_POST() throws Exception {
    mockMvc.perform(post("/manage/auth/reload").with(csrf())).andExpect(status().isOk());

    verify(auditor).audit(any(Runnable.class), any(Principal.class), eq("RELOAD_OIDC"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void reloadAuth_POST_forbidden_for_non_su() throws Exception {
    mockMvc.perform(post("/manage/auth/reload").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void reloadAuth_POST_unauthenticated() throws Exception {
    mockMvc.perform(post("/manage/auth/reload")).andExpect(status().isUnauthorized());
  }

  // ── PUT /manage/oidc ──────────────────────────────────────────────────────

  @Test
  @WithMockUser(roles = "SU")
  void projectsUpsert_PUT() throws Exception {
    var oidcDetails =
        OidcDetails.create("https://issuer.example.com", "my-client-id", "my-client-secret");

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
    // Empty body should fail @Valid validation
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
        OidcDetails.create("https://issuer.example.com", "my-client-id", "my-client-secret");

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
        OidcDetails.create("https://issuer.example.com", "my-client-id", "my-client-secret");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
