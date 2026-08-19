package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectExecCmd;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.ContainerNotFoundException;
import org.molgenis.armadillo.exceptions.DataPushFailedException;

@ExtendWith(MockitoExtension.class)
class FlowerDockerServiceTest {

  @Mock DockerClient dockerClient;
  @Mock DockerService dockerService;
  @Mock CopyArchiveToContainerCmd copyCmd;
  @Mock ExecCreateCmd execCreateCmd;
  @Mock ExecCreateCmdResponse execCreateCmdResponse;
  @Mock ExecStartCmd execStartCmd;
  @Mock ResultCallback.Adapter adapter;
  @Mock InspectExecCmd inspectExecCmd;
  @Mock InspectExecResponse inspectExecResponse;

  @Captor ArgumentCaptor<InputStream> tarStreamCaptor;

  private FlowerDockerService flowerDockerService;

  @BeforeEach
  void setup() {
    flowerDockerService = new FlowerDockerService(dockerClient, dockerService);
  }

  private void mockEnsureDirectoryExists(String containerName) throws InterruptedException {
    when(dockerService.asContainerName(containerName)).thenReturn(containerName);
    when(dockerClient.execCreateCmd(containerName)).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withCmd("mkdir", "-p", "/tmp/armadillo_data")).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenReturn(execCreateCmdResponse);
    when(execCreateCmdResponse.getId()).thenReturn("exec-id");
    when(dockerClient.execStartCmd("exec-id")).thenReturn(execStartCmd);
    when(execStartCmd.exec(any())).thenReturn(adapter);
    when(adapter.awaitCompletion()).thenReturn(adapter);
    when(dockerClient.inspectExecCmd("exec-id")).thenReturn(inspectExecCmd);
    when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
    when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);
  }

  @Test
  void copyDataToContainer_success() throws InterruptedException {
    mockEnsureDirectoryExists("my-container");
    when(dockerClient.copyArchiveToContainerCmd("my-container")).thenReturn(copyCmd);
    when(copyCmd.withTarInputStream(any())).thenReturn(copyCmd);
    when(copyCmd.withRemotePath("/tmp/armadillo_data")).thenReturn(copyCmd);

    byte[] testData = "test content".getBytes();
    InputStream data = new ByteArrayInputStream(testData);

    flowerDockerService.copyDataToContainer(
        "my-container", "/tmp/armadillo_data", "proj_data", data);

    verify(dockerClient).copyArchiveToContainerCmd("my-container");
    verify(copyCmd).withTarInputStream(any());
    verify(copyCmd).withRemotePath("/tmp/armadillo_data");
    verify(copyCmd).exec();
  }

  @Test
  void copyDataToContainer_containerNotFound() throws InterruptedException {
    mockEnsureDirectoryExists("missing");
    when(dockerClient.copyArchiveToContainerCmd("missing")).thenReturn(copyCmd);
    when(copyCmd.withTarInputStream(any())).thenReturn(copyCmd);
    when(copyCmd.withRemotePath("/tmp/armadillo_data")).thenReturn(copyCmd);
    doThrow(new NotFoundException("not found")).when(copyCmd).exec();

    InputStream data = new ByteArrayInputStream("data".getBytes());

    assertThrows(
        ContainerNotFoundException.class,
        () ->
            flowerDockerService.copyDataToContainer(
                "missing", "/tmp/armadillo_data", "file", data));
  }

  @Test
  void copyDataToContainer_dockerError() throws InterruptedException {
    mockEnsureDirectoryExists("broken");
    when(dockerClient.copyArchiveToContainerCmd("broken")).thenReturn(copyCmd);
    when(copyCmd.withTarInputStream(any())).thenReturn(copyCmd);
    when(copyCmd.withRemotePath("/tmp/armadillo_data")).thenReturn(copyCmd);
    doThrow(new DockerException("connection refused", 500)).when(copyCmd).exec();

    InputStream data = new ByteArrayInputStream("data".getBytes());

    assertThrows(
        DataPushFailedException.class,
        () ->
            flowerDockerService.copyDataToContainer("broken", "/tmp/armadillo_data", "file", data));
  }

  @Test
  void copyDataToContainer_resolvesDockerContainerName() throws InterruptedException {
    when(dockerService.asContainerName("logical-name")).thenReturn("prefix-logical-name-1");
    when(dockerClient.execCreateCmd("prefix-logical-name-1")).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withCmd("mkdir", "-p", "/tmp/armadillo_data")).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenReturn(execCreateCmdResponse);
    when(execCreateCmdResponse.getId()).thenReturn("exec-id");
    when(dockerClient.execStartCmd("exec-id")).thenReturn(execStartCmd);
    when(execStartCmd.exec(any())).thenReturn(adapter);
    when(adapter.awaitCompletion()).thenReturn(adapter);
    when(dockerClient.inspectExecCmd("exec-id")).thenReturn(inspectExecCmd);
    when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
    when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);
    when(dockerClient.copyArchiveToContainerCmd("prefix-logical-name-1")).thenReturn(copyCmd);
    when(copyCmd.withTarInputStream(any())).thenReturn(copyCmd);
    when(copyCmd.withRemotePath("/tmp/armadillo_data")).thenReturn(copyCmd);

    InputStream data = new ByteArrayInputStream("data".getBytes());

    flowerDockerService.copyDataToContainer("logical-name", "/tmp/armadillo_data", "file", data);

    verify(dockerClient).execCreateCmd("prefix-logical-name-1");
    verify(dockerClient).copyArchiveToContainerCmd("prefix-logical-name-1");
  }

  @Test
  void copyDataToContainer_mkdirNonZeroExitCodeThrows() throws InterruptedException {
    when(dockerService.asContainerName("my-container")).thenReturn("my-container");
    when(dockerClient.execCreateCmd("my-container")).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStdout(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withAttachStderr(true)).thenReturn(execCreateCmd);
    when(execCreateCmd.withCmd("mkdir", "-p", "/tmp/armadillo_data")).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenReturn(execCreateCmdResponse);
    when(execCreateCmdResponse.getId()).thenReturn("exec-id");
    when(dockerClient.execStartCmd("exec-id")).thenReturn(execStartCmd);
    when(execStartCmd.exec(any())).thenReturn(adapter);
    when(adapter.awaitCompletion()).thenReturn(adapter);
    when(dockerClient.inspectExecCmd("exec-id")).thenReturn(inspectExecCmd);
    when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
    when(inspectExecResponse.getExitCodeLong()).thenReturn(1L);

    InputStream data = new ByteArrayInputStream("data".getBytes());

    assertThrows(
        DataPushFailedException.class,
        () ->
            flowerDockerService.copyDataToContainer(
                "my-container", "/tmp/armadillo_data", "file", data));

    verify(dockerClient, never()).copyArchiveToContainerCmd(any());
  }

  @Test
  void createTarArchive_roundTrip() throws IOException {
    byte[] content = "hello world".getBytes();
    String fileName = "test_file.parquet";

    InputStream tarStream = FlowerDockerService.createTarArchive(fileName, content);
    byte[] tarBytes = tarStream.readAllBytes();

    try (TarArchiveInputStream tarInput =
        new TarArchiveInputStream(new ByteArrayInputStream(tarBytes))) {
      TarArchiveEntry entry = tarInput.getNextEntry();
      assertNotNull(entry);
      assertEquals(fileName, entry.getName());
      assertEquals(content.length, entry.getSize());

      byte[] extracted = tarInput.readAllBytes();
      assertArrayEquals(content, extracted);

      assertNull(tarInput.getNextEntry());
    }
  }
}
