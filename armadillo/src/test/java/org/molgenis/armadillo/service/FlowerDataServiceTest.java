package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.FlowerDockerService;
import org.molgenis.armadillo.container.FlowerSuperExecContainerConfig;
import org.molgenis.armadillo.exceptions.NotFlowerSuperexecContainerException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;

@ExtendWith(MockitoExtension.class)
class FlowerDataServiceTest {

  @Mock ArmadilloStorageService storageService;
  @Mock FlowerDockerService flowerDockerService;
  @Mock ContainerService containerService;

  private FlowerDataService flowerDataService;

  @BeforeEach
  void setup() {
    flowerDataService =
        new FlowerDataService(storageService, flowerDockerService, containerService);
  }

  @Test
  void pushData_success() throws Exception {
    var flowerClient = mock(FlowerSuperExecContainerConfig.class);
    when(containerService.getByName("flower-client-1")).thenReturn(flowerClient);
    InputStream data = new ByteArrayInputStream("content".getBytes());
    when(storageService.loadObject("myproject", "train.parquet")).thenReturn(data);
    when(storageService.getFileSizeIfObjectExists("shared-myproject", "train.parquet"))
        .thenReturn(7L);

    flowerDataService.pushData("myproject", "train.parquet", "flower-client-1");

    verify(storageService).loadObject("myproject", "train.parquet");
    verify(flowerDockerService)
        .copyDataToContainer(
            "flower-client-1", "/tmp/armadillo_data", "myproject_train.parquet", data, 7L);
  }

  @Test
  void pushData_encodesResourcePath() throws Exception {
    var flowerClient = mock(FlowerSuperExecContainerConfig.class);
    when(containerService.getByName("container-1")).thenReturn(flowerClient);
    InputStream data = new ByteArrayInputStream("content".getBytes());
    when(storageService.loadObject("proj", "data/train")).thenReturn(data);
    when(storageService.getFileSizeIfObjectExists("shared-proj", "data/train")).thenReturn(7L);

    flowerDataService.pushData("proj", "data/train", "container-1");

    verify(flowerDockerService)
        .copyDataToContainer("container-1", "/tmp/armadillo_data", "proj_data%2Ftrain", data, 7L);
  }

  @Test
  void pushData_doesNotCollideOnSlashVsUnderscore() throws Exception {
    var flowerClient = mock(FlowerSuperExecContainerConfig.class);
    when(containerService.getByName("container-1")).thenReturn(flowerClient);
    InputStream dataA = new ByteArrayInputStream("a".getBytes());
    InputStream dataB = new ByteArrayInputStream("b".getBytes());
    when(storageService.loadObject("proj", "data/train")).thenReturn(dataA);
    when(storageService.loadObject("proj", "data_train")).thenReturn(dataB);
    when(storageService.getFileSizeIfObjectExists("shared-proj", "data/train")).thenReturn(1L);
    when(storageService.getFileSizeIfObjectExists("shared-proj", "data_train")).thenReturn(1L);

    flowerDataService.pushData("proj", "data/train", "container-1");
    flowerDataService.pushData("proj", "data_train", "container-1");

    verify(flowerDockerService)
        .copyDataToContainer("container-1", "/tmp/armadillo_data", "proj_data%2Ftrain", dataA, 1L);
    verify(flowerDockerService)
        .copyDataToContainer("container-1", "/tmp/armadillo_data", "proj_data_train", dataB, 1L);
  }

  @Test
  void pushData_rejectsNonFlowerContainer() {
    var datashieldContainer = mock(DatashieldContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(datashieldContainer);

    assertThrows(
        NotFlowerSuperexecContainerException.class,
        () -> flowerDataService.pushData("myproject", "train.parquet", "default"));

    verify(flowerDockerService, never()).copyDataToContainer(any(), any(), any(), any(), anyLong());
  }
}
