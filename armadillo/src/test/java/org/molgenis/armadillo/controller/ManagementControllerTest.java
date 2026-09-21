package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.molgenis.armadillo.audit.AuditEventPublisher;
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
}
