package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.core.DockerClientImpl;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerClientConfigTest {

  @Mock private DockerClient dockerClient;
  @Mock private InfoCmd infoCmd;

  @Test
  void getDockerClient_returnsClientWhenInfoSucceeds() {
    when(dockerClient.infoCmd()).thenReturn(infoCmd);
    when(infoCmd.exec()).thenReturn(null);

    try (MockedStatic<DockerClientImpl> dockerClientImpl = mockStatic(DockerClientImpl.class)) {
      dockerClientImpl
          .when(() -> DockerClientImpl.getInstance(any(), any()))
          .thenReturn(dockerClient);

      DockerClientConfig config = new DockerClientConfig();
      DockerClient result = config.getDockerClient();

      assertSame(dockerClient, result);
      verify(infoCmd).exec();
    }
  }

  @Test
  void getDockerClient_returnsClientWhenInfoFails() {
    when(dockerClient.infoCmd()).thenReturn(infoCmd);
    when(infoCmd.exec()).thenThrow(new ProcessingException("down"));

    try (MockedStatic<DockerClientImpl> dockerClientImpl = mockStatic(DockerClientImpl.class)) {
      dockerClientImpl
          .when(() -> DockerClientImpl.getInstance(any(), any()))
          .thenReturn(dockerClient);

      DockerClientConfig config = new DockerClientConfig();
      DockerClient result = config.getDockerClient();

      assertSame(dockerClient, result);
      verify(infoCmd).exec();
    }
  }
}
