package org.molgenis.armadillo.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentUserController.class)
class CurrentUserControllerTest extends ArmadilloControllerTestBase {

  @MockBean DockerClient dockerClient;
  @Autowired private MockMvc mockMvc;

  @MockBean ArmadilloStorageService armadilloStorage;
  @MockBean OAuth2AuthorizedClientService auth2AuthorizedClientService;

  @Test
  @WithMockJwtAuth(
      authorities = "ROLE_myproject_RESEARCHER",
      claims = @OpenIdClaims(email = "bofke@email.com"))
  void currentUser_permissions_GET() throws Exception {
    mockMvc
        .perform(get("/my/projects"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"myproject\"]"));
  }

  @Test
  @WithMockUser
  void currentUser_GET_WhenUserHasNoGrantsTest() throws Exception {
    mockMvc
        .perform(get("/my/projects"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  void testNotAuthenticated() throws Exception {
    mockMvc.perform(get("/my/principal")).andExpect(status().isUnauthorized());
  }
}
