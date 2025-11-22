package org.molgenis.armadillo.controller;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(DevelopmentController.class)
@Import({TestSecurityConfig.class})
class DevelopmentControllerTest extends ArmadilloControllerTestBase {

  @MockitoBean private ContainerService containerService;
  @MockitoBean private Commands commands;
  @MockitoBean private ArmadilloStorageService armadilloStorage;

  @Mock(lenient = true)
  private Clock clock;

  @Captor private ArgumentCaptor<AuditApplicationEvent> eventCaptor;
  MockHttpSession session = new MockHttpSession();
  private final Instant instant = Instant.now();
  private String sessionId;
  private AuditEventValidator auditEventValidator;
  @MockitoBean private DockerService dockerService;

  @BeforeEach
  public void setup() {
    auditEventValidator = new AuditEventValidator(applicationEventPublisher, eventCaptor);
    auditEventPublisher.setClock(clock);
    auditEventPublisher.setApplicationEventPublisher(applicationEventPublisher);
    when(clock.instant()).thenReturn(instant);
    sessionId = session.changeSessionId();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testInstallPackageSu() throws Exception {
    String filename = "hello.txt";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", filename, MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());
    when(commands.installPackage(any(Principal.class), any(Resource.class), any(String.class)))
        .thenReturn(completedFuture(null));
    mockMvc
        .perform(MockMvcRequestBuilders.multipart("/install-package").file(file))
        .andExpect(status().is(204));
  }

  @Test
  @WithMockUser
  void testInstallPackageUser() throws Exception {
    String filename = "hello.txt";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", filename, MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());
    when(commands.installPackage(any(Principal.class), any(Resource.class), any(String.class)))
        .thenReturn(completedFuture(null));
    mockMvc
        .perform(MockMvcRequestBuilders.multipart("/install-package").file(file))
        .andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testInstallPackageFileNull() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", null, MediaType.TEXT_PLAIN_VALUE, "".getBytes());
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/install-package").file(file).session(session))
            .andExpect(status().is(204))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().is(500));
    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            "INSTALL_PACKAGES_FAILURE",
            Map.of(
                "sessionId",
                sessionId,
                "roles",
                List.of("ROLE_SU"),
                "message",
                "Filename is null or empty")));
  }

  @Test
  void testGetPackageNameFromFilename() {
    String filename = "hello_world_test.tar.gz";
    DevelopmentController controller =
        new DevelopmentController(commands, auditEventPublisher, containerService, dockerService);
    String pkgName = controller.getPackageNameFromFilename(filename);
    assertEquals("hello_world", pkgName);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteDockerImage() throws Exception {
    String imageId = "some-image-id";

    // We mock the dockerService.removeImageIfUnused to do nothing (void method)
    // You can verify later if needed.
    doNothing().when(dockerService).removeImageIfUnused(imageId);

    mockMvc
        .perform(MockMvcRequestBuilders.delete("/delete-docker-image").param("imageId", imageId))
        .andExpect(status().isNoContent());

    // Verify that dockerService.removeImageIfUnused was called with the correct imageId
    verify(dockerService).removeImageIfUnused(imageId);
  }
}
