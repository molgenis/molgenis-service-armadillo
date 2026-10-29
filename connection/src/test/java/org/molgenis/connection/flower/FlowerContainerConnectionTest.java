package org.molgenis.connection.flower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.connection.exceptions.DataPushFailedException;
import org.molgenis.connection.flower.helpers.TarFile;
import org.molgenis.connection.flower.service.FlowerDockerService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FlowerContainerConnectionTest {

  private static final String CONTAINER_NAME = "flower-node";
  private static final String DATA_DIR = "/tmp/armadillo_data";

  @Mock private FlowerDockerService dockerService;

  private FlowerContainerConnection connection;

  @BeforeEach
  void setUp() {
    connection = new FlowerContainerConnection(CONTAINER_NAME, dockerService);
    // @Value fields are not injected outside a Spring context, so set them explicitly
    setConfig(false, "");
  }

  private void setConfig(boolean runningInContainer, String prefix) {
    ReflectionTestUtils.setField(connection, "isArmadilloRunningInContainer", runningInContainer);
    ReflectionTestUtils.setField(connection, "containerPrefix", prefix);
  }

  private static InputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void getContainerName_notInContainerAndNoPrefix_returnsPlainName() {
    setConfig(false, "");

    assertEquals(CONTAINER_NAME, connection.getContainerName());
  }

  @Test
  void getContainerName_notInContainerWithPrefix_returnsPlainName() {
    setConfig(false, "armadillo-");

    assertEquals(CONTAINER_NAME, connection.getContainerName());
  }

  @Test
  void getContainerName_inContainerButEmptyPrefix_returnsPlainName() {
    setConfig(true, "");

    assertEquals(CONTAINER_NAME, connection.getContainerName());
  }

  @Test
  void getContainerName_inContainerWithPrefix_returnsPrefixedNameWithSuffix() {
    setConfig(true, "armadillo-");

    assertEquals("armadillo-" + CONTAINER_NAME + "-1", connection.getContainerName());
  }

  @Test
  void pushData_copiesTarToPlainContainerName() {
    String content = "hello flower";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    connection.pushData("data.csv", stream(content), bytes.length);

    verify(dockerService)
        .copyCompressedDataToContainer(eq(CONTAINER_NAME), eq(DATA_DIR), any(TarFile.class));
  }

  @Test
  void pushData_usesPrefixedContainerNameWhenRunningInContainer() {
    setConfig(true, "armadillo-");
    String content = "hello flower";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    connection.pushData("data.csv", stream(content), bytes.length);

    verify(dockerService)
        .copyCompressedDataToContainer(
            eq("armadillo-" + CONTAINER_NAME + "-1"), eq(DATA_DIR), any(TarFile.class));
  }

  @Test
  void pushData_passesTarFileToDockerService() {
    String content = "some,csv,data";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    ArgumentCaptor<TarFile> captor = ArgumentCaptor.forClass(TarFile.class);

    connection.pushData("data.csv", stream(content), bytes.length);

    verify(dockerService)
        .copyCompressedDataToContainer(eq(CONTAINER_NAME), eq(DATA_DIR), captor.capture());
    assertNotNull(captor.getValue());
  }

  @Test
  void pushData_exceptionFromDockerService_propagatesUnchanged() {
    String content = "hello flower";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    InputStream in = stream(content);
    long size = bytes.length;
    DataPushFailedException expected =
        new DataPushFailedException(CONTAINER_NAME, new IOException("docker copy failed"));
    doThrow(expected)
        .when(dockerService)
        .copyCompressedDataToContainer(any(String.class), any(String.class), any(TarFile.class));

    DataPushFailedException thrown =
        assertThrows(
            DataPushFailedException.class, () -> connection.pushData("data.csv", in, size));

    assertSame(expected, thrown);
  }
}
