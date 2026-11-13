package org.molgenis.connection.flower.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.connection.flower.FlowerContainerConnection;
import org.molgenis.connection.flower.FlowerServerConnection;

/**
 * FlowerExecutorService creates its connections with "new", so they are intercepted with
 * Mockito.mockConstruction. Constructor arguments are captured in the initializer callback via
 * context.arguments().
 */
@ExtendWith(MockitoExtension.class)
class FlowerExecutorServiceTest {

  @Mock FlowerDockerService dockerService;

  FlowerExecutorService executorService;

  // constructor arguments per constructed instance, in construction order
  List<List<?>> containerArgs;
  List<List<?>> serverArgs;

  @BeforeEach
  void setUp() {
    executorService = new FlowerExecutorService(dockerService);
    containerArgs = new ArrayList<>();
    serverArgs = new ArrayList<>();
  }

  private MockedConstruction<FlowerContainerConnection> mockContainerConnections() {
    return Mockito.mockConstruction(
        FlowerContainerConnection.class, (mock, context) -> containerArgs.add(context.arguments()));
  }

  private MockedConstruction<FlowerServerConnection> mockServerConnections() {
    return Mockito.mockConstruction(
        FlowerServerConnection.class, (mock, context) -> serverArgs.add(context.arguments()));
  }

  @Test
  void loadData_createsContainerConnectionWithContainerNameAndDockerService() {
    InputStream data = new ByteArrayInputStream(new byte[] {1, 2, 3});

    try (MockedConstruction<FlowerContainerConnection> containerConstruction =
            mockContainerConnections();
        MockedConstruction<FlowerServerConnection> serverConstruction = mockServerConnections()) {

      executorService.loadData("my-container", "project/folder/file.parquet", data);

      assertEquals(1, containerConstruction.constructed().size());
      assertEquals("my-container", containerArgs.get(0).get(0));
      assertSame(dockerService, containerArgs.get(0).get(1));
    }
  }

  @Test
  void loadData_wrapsContainerConnectionInServerConnection() {
    InputStream data = new ByteArrayInputStream(new byte[] {1});

    try (MockedConstruction<FlowerContainerConnection> containerConstruction =
            mockContainerConnections();
        MockedConstruction<FlowerServerConnection> serverConstruction = mockServerConnections()) {

      executorService.loadData("my-container", "project/folder/file.parquet", data);

      assertEquals(1, serverConstruction.constructed().size());
      FlowerContainerConnection createdContainerConnection =
          containerConstruction.constructed().get(0);
      assertSame(createdContainerConnection, serverArgs.get(0).get(0));
    }
  }

  @Test
  void loadData_writesFileWithGivenPathAndStream() {
    InputStream data = new ByteArrayInputStream(new byte[] {1, 2});

    try (MockedConstruction<FlowerContainerConnection> containerConstruction =
            mockContainerConnections();
        MockedConstruction<FlowerServerConnection> serverConstruction = mockServerConnections()) {

      executorService.loadData("my-container", "project/folder/file.parquet", data);

      FlowerServerConnection serverConnection = serverConstruction.constructed().get(0);
      verify(serverConnection).writeFile("project/folder/file.parquet", data);
    }
  }

  @Test
  void loadData_createsNewConnectionsForEveryCall() {
    InputStream data1 = new ByteArrayInputStream(new byte[] {1});
    InputStream data2 = new ByteArrayInputStream(new byte[] {2});

    try (MockedConstruction<FlowerContainerConnection> containerConstruction =
            mockContainerConnections();
        MockedConstruction<FlowerServerConnection> serverConstruction = mockServerConnections()) {

      executorService.loadData("container-a", "p/f/a.parquet", data1);
      executorService.loadData("container-b", "p/f/b.parquet", data2);

      assertEquals(2, containerConstruction.constructed().size());
      assertEquals(2, serverConstruction.constructed().size());
      assertEquals("container-a", containerArgs.get(0).get(0));
      assertEquals("container-b", containerArgs.get(1).get(0));
      verify(serverConstruction.constructed().get(0)).writeFile("p/f/a.parquet", data1);
      verify(serverConstruction.constructed().get(1)).writeFile("p/f/b.parquet", data2);
    }
  }

  @Test
  void loadData_propagatesExceptionFromWriteFile() {
    InputStream data = new ByteArrayInputStream(new byte[] {1});

    try (MockedConstruction<FlowerContainerConnection> containerConstruction =
            mockContainerConnections();
        MockedConstruction<FlowerServerConnection> serverConstruction =
            Mockito.mockConstruction(
                FlowerServerConnection.class,
                (mock, context) ->
                    doThrow(new IllegalArgumentException("boom"))
                        .when(mock)
                        .writeFile(anyString(), any(InputStream.class)))) {

      IllegalArgumentException thrown =
          assertThrows(
              IllegalArgumentException.class,
              () -> executorService.loadData("my-container", "project/folder/file.parquet", data));

      assertEquals("boom", thrown.getMessage());
    }
  }
}
