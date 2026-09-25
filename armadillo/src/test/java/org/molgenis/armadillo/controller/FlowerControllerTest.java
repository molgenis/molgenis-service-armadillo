package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.verify;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.service.FlowerDataService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(FlowerController.class)
@Import({TestSecurityConfig.class})
@TestPropertySource(properties = "armadillo.docker-management-enabled=true")
class FlowerControllerTest extends ArmadilloControllerTestBase {

  @MockitoBean FlowerDataService flowerDataService;
  @MockitoBean ArmadilloStorageService armadilloStorageService;

  private static final String PUSH_DATA_URL = "/flower/push-data";
  private static final String VALID_BODY =
      """
      {"project":"myproject","resource":"train.parquet","containerName":"flower-client-1"}""";

  @Test
  @WithMockUser(roles = {"MYPROJECT_RESEARCHER"})
  void pushData_204() throws Exception {
    mockMvc
        .perform(
            post(PUSH_DATA_URL).session(session).contentType(APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isNoContent());

    verify(flowerDataService).pushData("myproject", "train.parquet", "flower-client-1");
    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            FLOWER_PUSH_DATA,
            mockAuditMap(
                Map.of(
                    PROJECT, "myproject",
                    RESOURCE, "train.parquet",
                    CONTAINER, "flower-client-1"))));
  }

  @Test
  @WithMockUser(roles = {"SU"})
  void pushData_204_asSuperUser() throws Exception {
    mockMvc
        .perform(
            post(PUSH_DATA_URL).session(session).contentType(APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isNoContent());

    verify(flowerDataService).pushData("myproject", "train.parquet", "flower-client-1");
  }

  @Test
  void pushData_401_whenUnauthenticated() throws Exception {
    mockMvc
        .perform(post(PUSH_DATA_URL).contentType(APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"MYPROJECT_RESEARCHER"})
  void pushData_400_whenBodyInvalid() throws Exception {
    String invalidBody =
        """
        {"project":"","resource":"train.parquet","containerName":"flower-client-1"}""";
    mockMvc
        .perform(
            post(PUSH_DATA_URL).session(session).contentType(APPLICATION_JSON).content(invalidBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = {"MYPROJECT_RESEARCHER"})
  void pushData_400_whenBodyMissing() throws Exception {
    mockMvc
        .perform(post(PUSH_DATA_URL).session(session).contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  private Map<String, Object> mockAuditMap(Map<String, Object> additionalValues) {
    var values = new HashMap<String, Object>();
    values.put("sessionId", sessionId);
    values.put("roles", List.of("ROLE_MYPROJECT_RESEARCHER"));
    values.putAll(additionalValues);
    return values;
  }
}
