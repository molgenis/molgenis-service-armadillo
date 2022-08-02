package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.settings.ArmadilloSettingsService.SETTINGS_FILE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.settings.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SettingsController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class})
public class SettingsControllerTest {

  public static final String BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON =
      "{\"users\":{\"bofke@email.com\":{\"projects\":[\"myproject\"]}}}";
  @MockBean private ArmadilloStorageService armadilloStorage;

  @Autowired AuditEventPublisher auditEventPublisher;
  @Captor private ArgumentCaptor<AuditApplicationEvent> eventCaptor;
  @MockBean private ApplicationEventPublisher applicationEventPublisher;

  @Mock(lenient = true)
  private Clock clock;

  private final Instant instant = Instant.now();
  @Autowired private MockMvc mockMvc;

  MockHttpSession session = new MockHttpSession();
  private String sessionId;

  @MockBean JwtDecoder jwtDecoder;

  @BeforeEach
  public void setup() {
    AuditEventValidator auditEventValidator =
        new AuditEventValidator(applicationEventPublisher, eventCaptor);
    auditEventPublisher.setClock(clock);
    auditEventPublisher.setApplicationEventPublisher(applicationEventPublisher);
    when(clock.instant()).thenReturn(instant);
    sessionId = session.changeSessionId();
  }

  @Test
  @WithMockUser(roles = "SU")
  public void access_GET_WhenNoGrantsAreSaved() throws Exception {
    // when first time then permissions file will be missing
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE)).thenReturn(InputStream.nullInputStream());

    mockMvc
        .perform(get("/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void access_GET() throws Exception {
    final String JSON = BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON;
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc
        .perform(get("/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{\"myproject\":[\"bofke@email.com\"]}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  @WithUserDetails("bofke")
  public void access_email_GET() throws Exception {
    final String JSON = BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON;
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(JSON.getBytes()));

    mockMvc
        .perform(get("/access/bofke@email.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"myproject\"]"));
  }

  @Test
  @WithMockUser
  public void access_GET_PermissionDenied() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc.perform(get("/access")).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void access_POST() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

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
        .saveSystemFile(argument.capture(), eq(SETTINGS_FILE), eq(APPLICATION_JSON));
    assertEquals(
        BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON, new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void access_DELETE() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            delete("/access")
                .param("email", "bofke@email.com")
                .param("project", "myproject")
                .with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(SETTINGS_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"projects\":[]}}}",
        new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockJwtAuth(
      authorities = "ROLE_myproject_RESEARCHER",
      claims = @OpenIdClaims(email = "bofke@email.com"))
  public void my_access_GET() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc
        .perform(get("/my/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"myproject\"]"));
  }

  @Test
  @WithMockUser
  public void my_access_GET_WhenUserHasNoGrantsTest() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc
        .perform(get("/my/access"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void users_GET() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    mockMvc
        .perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("{\"bofke@email.com\":{\"projects\":[\"myproject\"]}}"));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void users_PUT() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream("{}".getBytes()));

    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc
        .perform(
            put("/users/bofke@email.com")
                .content(
                    new Gson()
                        .toJson(
                            new UserDetails("first", "last", "myInstitution", Set.of("myproject"))))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(SETTINGS_FILE), eq(APPLICATION_JSON));
    assertEquals(
        "{\"users\":{\"bofke@email.com\":{\"firstName\":\"first\",\"lastName\":\"last\",\"institution\":\"myInstitution\",\"projects\":[\"myproject\"]}}}",
        new String(argument.getValue().readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  public void users_DELETE() throws Exception {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(BOFKE_EMAIL_COM_PROJECTS_MYPROJECT_JSON.getBytes()));

    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    mockMvc.perform(delete("/users/bofke@email.com").with(csrf())).andExpect(status().isOk());

    // verify mock magic, I must say I prefer integration tests above this nonsense
    verify(armadilloStorage)
        .saveSystemFile(argument.capture(), eq(SETTINGS_FILE), eq(APPLICATION_JSON));
    assertEquals("{\"users\":{}}", new String(argument.getValue().readAllBytes()));
  }
}
