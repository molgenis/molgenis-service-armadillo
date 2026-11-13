package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.verify;
import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.START_CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.STOP_CONTAINER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.container.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ContainerDockerController.class)
@Import({TestSecurityConfig.class})
@TestPropertySource(properties = "armadillo.docker-management-enabled=true")
class ContainerDockerControllerTest extends ArmadilloControllerTestBase {

  @MockitoBean DockerService dockerService;
  @MockitoBean ArmadilloStorageService armadilloStorageService;

  @Test
  @WithMockUser(roles = "SU")
  void startContainer_POST() throws Exception {
    mockMvc
        .perform(post("/containers/donkey/start").session(session).with(csrf()))
        .andExpect(status().isNoContent());

    verify(dockerService).pullImageStartContainer("donkey");
    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant, "user", START_CONTAINER, mockSuAuditMap(Map.of(CONTAINER, "donkey"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void stopContainer_POST() throws Exception {
    mockMvc
        .perform(post("/containers/donkey/stop").session(session).with(csrf()))
        .andExpect(status().isNoContent());

    verify(dockerService).stopAndRemoveContainer("donkey");
    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant, "user", STOP_CONTAINER, mockSuAuditMap(Map.of(CONTAINER, "donkey"))));
  }

  private Map<String, Object> mockSuAuditMap(Map<String, Object> additionalValues) {
    var values = new HashMap<String, Object>();
    values.put("sessionId", sessionId);
    values.put("roles", List.of("ROLE_SU"));
    values.putAll(additionalValues);
    return values;
  }
}
