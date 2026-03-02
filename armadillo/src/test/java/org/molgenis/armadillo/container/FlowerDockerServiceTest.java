package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
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
  @Mock CopyArchiveToContainerCmd copyCmd;

  @Captor ArgumentCaptor<InputStream> tarStreamCaptor;

  private FlowerDockerService flowerDockerService;

  @BeforeEach
  void setup() {
    flowerDockerService = new FlowerDockerService(dockerClient);
  }

  @Test
  void copyDataToContainer_success() {
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
  void copyDataToContainer_containerNotFound() {
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
  void copyDataToContainer_dockerError() {
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
