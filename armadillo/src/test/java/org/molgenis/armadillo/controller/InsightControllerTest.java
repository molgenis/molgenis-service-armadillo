package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class, TestSecurityConfig.class})
public class InsightControllerTest {

  @Autowired AuditEventPublisher auditEventPublisher;
  @Autowired MockMvc mockMvc;
  @MockBean JwtDecoder jwtDecoder;
  @Autowired InsightService insightService;

  //    @BeforeEach
  //    public void before() {
  ////        runAsSystem(() -> insightService.initialize());
  //    }

  @Test
  public void testFilesList() throws Exception {
    mockMvc.perform(get("/insight")).andExpect(status().isOk());
  }
}
