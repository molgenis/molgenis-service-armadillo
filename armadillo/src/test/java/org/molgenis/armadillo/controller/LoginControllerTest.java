package org.molgenis.armadillo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

  @MockitoBean AccessService accessService;
  @Autowired MockMvc mockMvc;

  // -------------------------------------------------------------------------
  // GET /oauth2/
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  void whenAuthenticatedRedirect_redirectsToRoot() throws Exception {
    mockMvc
        .perform(get("/oauth2/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  // -------------------------------------------------------------------------
  // GET /basic-login
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  void basicLogin_redirectsToRoot() throws Exception {
    mockMvc
        .perform(get("/basic-login"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  // -------------------------------------------------------------------------
  // GET /basic-logout
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  void basicLogout_returns401() throws Exception {
    mockMvc.perform(get("/basic-logout")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void basicLogout_setsWwwAuthenticateHeader() throws Exception {
    mockMvc
        .perform(get("/basic-logout"))
        .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Armadillo\""));
  }

  @Test
  @WithAnonymousUser
  void basicLogout_isAccessibleWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/basic-logout")).andExpect(status().isUnauthorized());
  }
}
