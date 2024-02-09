package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.InsightService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class, TestSecurityConfig.class})
@WithMockUser(roles = "SU")
public class InsightControllerTest {

  @Autowired AuditEventPublisher auditEventPublisher;
  @Autowired MockMvc mockMvc;
  @MockBean JwtDecoder jwtDecoder;
  @Autowired InsightService insightService;

  @MockBean ArmadilloStorageService armadilloStorage;
  @Autowired AccessService accessService;

  @BeforeEach
  public void before() {
    //      runAsSystem(() -> insightService.initialize());
  }

  @Test
  public void testFilesList() throws Exception {
    mockMvc
        .perform(get("/insight/files"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
  }

  @Test
  public void testFilesDetail() throws Exception {
    mockMvc
        .perform(get("/insight/files/XyZ"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("XyZ"))
        .andExpect(jsonPath("$.name").value("XyZ"))
        .andExpect(jsonPath("$.content").value("XyZ"));
  }
}
