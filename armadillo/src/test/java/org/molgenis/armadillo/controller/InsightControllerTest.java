package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.dockerjava.api.model.Image;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.InsightService;
import org.molgenis.armadillo.model.DockerImageInfo;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import({AuditEventPublisher.class, TestSecurityConfig.class})
@WithMockUser(roles = "SU")
public class InsightControllerTest {

  @Autowired AuditEventPublisher auditEventPublisher;
  @Autowired MockMvc mockMvc;
  @MockitoBean JwtDecoder jwtDecoder;
  @Autowired InsightService insightService;

  @MockitoBean ArmadilloStorageService armadilloStorage;
  @Autowired AccessService accessService;

  @MockitoBean DockerService dockerService;

  @Test
  void testFilesList() throws Exception {
    mockMvc
        .perform(get("/insight/files"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
  }

  @Test
  void testFilesDetail() throws Exception {
    mockMvc
        .perform(get("/insight/files/XyZ"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("XyZ"))
        .andExpect(jsonPath("$.name").value("XyZ"))
        .andExpect(jsonPath("$.content").value("XyZ"));
  }

  @Test
  void testGetDockerImages() throws Exception {
    // Arrange
    Image mockImage = mock(Image.class);
    when(mockImage.getId()).thenReturn("sha256:1234");
    when(mockImage.getRepoTags()).thenReturn(new String[] {"my-image:latest"});
    when(mockImage.getSize()).thenReturn(11_000_000L); // 11 MB
    when(mockImage.getCreated()).thenReturn(1753712029L); // seconds since epoch

    DockerImageInfo imageInfo = DockerImageInfo.create(mockImage);
    when(dockerService.getDockerImages()).thenReturn(List.of(imageInfo));

    Principal mockPrincipal = () -> "test-user";

    // Act & Assert
    mockMvc
        .perform(get("/insight/docker/all-images").principal(mockPrincipal))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$[0].imageId").value("sha256:1234"))
        .andExpect(jsonPath("$[0].repoTags[0]").value("my-image:latest"));
  }
}
