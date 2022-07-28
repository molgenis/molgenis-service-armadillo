package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.security.AccessStorageService.PERMISSIONS_FILE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.security.AccessStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccessController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class, AccessController.class})
public class AccessControllerTest {

  @MockBean private ArmadilloStorageService armadilloStorage;

  private AuditEventValidator auditEventValidator;
  @Autowired AuditEventPublisher auditEventPublisher;
  @Captor private ArgumentCaptor<AuditApplicationEvent> eventCaptor;
  @MockBean private ApplicationEventPublisher applicationEventPublisher;

  @Mock(lenient = true)
  private Clock clock;

  private final Instant instant = Instant.now();
  @Autowired private MockMvc mockMvc;

  MockHttpSession session = new MockHttpSession();
  private String sessionId;

  @BeforeEach
  public void setup() {
    auditEventValidator = new AuditEventValidator(applicationEventPublisher, eventCaptor);
    auditEventPublisher.setClock(clock);
    auditEventPublisher.setApplicationEventPublisher(applicationEventPublisher);
    when(clock.instant()).thenReturn(instant);
    sessionId = session.changeSessionId();
  }

  @EnableGlobalMethodSecurity(prePostEnabled = true)
  @Configuration
  static class Config {
    @Bean
    AccessStorageService accessStorageService() {
      return new AccessStorageService();
    }
  }

  @Test
  @WithMockUser(roles = "SU")
  public void getAccessTestWhenNoGrantsAreSaved() throws Exception {
    // when first time then permissions file will be missing
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(InputStream.nullInputStream());

    mockMvc
        .perform(get("/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void getAccessTest() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc
        .perform(get("/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(JSON));
  }

  @Test
  @WithMockUser
  public void getAccessTestPermissionDenied() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc.perform(get("/access")).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void postAccessTest() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            post("/access")
                .param("project", "myproject")
                .param("email", "bofke@email.com")
                .with(csrf()))
        .andExpect(status().isCreated());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(PERMISSIONS_FILE), eq(APPLICATION_JSON));
    assertEquals(JSON, new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void deleteAccessTest() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            delete("/access")
                .param("project", "myproject")
                .param("email", "bofke@email.com")
                .with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(PERMISSIONS_FILE), eq(APPLICATION_JSON));
    assertEquals("{\"myproject\":[]}", new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockJwtAuth(
      authorities = "ROLE_myproject_RESEARCHER",
      claims = @OpenIdClaims(email = "bofke@email.com"))
  public void getMyAccessTest() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc
        .perform(get("/myAccess"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"myproject\"]"));
  }

  @Test
  @WithMockUser
  public void getMyAccessWhenUserHasNoGrantsTest() throws Exception {
    final String JSON = "{\"myproject\":[\"bofke@email.com\"]}";
    when(armadilloStorage.loadSystemFile(PERMISSIONS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc
        .perform(get("/myAccess"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }
}
