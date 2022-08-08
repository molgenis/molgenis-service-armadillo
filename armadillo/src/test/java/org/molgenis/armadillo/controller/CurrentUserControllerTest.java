package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentUserController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class})
public class CurrentUserControllerTest {

  public static final String BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON =
      "{\"users\":{\"bofke@email.com\":{\"accessToProjects\":[\"myproject\"]}}}";
  @MockBean private ArmadilloStorageService armadilloStorage;
  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockJwtAuth(
      authorities = "ROLE_myproject_RESEARCHER",
      claims = @OpenIdClaims(email = "bofke@email.com"))
  public void currentUser_permissions_GET() throws Exception {
    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc
        .perform(get("/currentUser/accessToProjects"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"myproject\"]"));
  }

  @Test
  @WithMockUser
  public void currentUser_GET_WhenUserHasNoGrantsTest() throws Exception {
    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc
        .perform(get("/currentUser/accessToProjects"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }
}
