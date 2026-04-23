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
import org.molgenis.armadillo.container.FlowerSuperexecContainerConfig;
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
  void pushData_success() {
    when(containerService.getByName("flower-client-1"))
        .thenReturn(mock(FlowerSuperexecContainerConfig.class));
    InputStream data = new ByteArrayInputStream("content".getBytes());
    when(storageService.loadObject("myproject", "train.parquet")).thenReturn(data);

    flowerDataService.pushData("myproject", "train.parquet", "flower-client-1");

    verify(storageService).loadObject("myproject", "train.parquet");
    verify(flowerDockerService)
        .copyDataToContainer(
            "flower-client-1", "/tmp/armadillo_data", "myproject_train.parquet", data);
  }

  @Test
  void pushData_sanitizesResourcePath() {
    when(containerService.getByName("container-1"))
        .thenReturn(mock(FlowerSuperexecContainerConfig.class));
    InputStream data = new ByteArrayInputStream("content".getBytes());
    when(storageService.loadObject("proj", "data/train")).thenReturn(data);

    flowerDataService.pushData("proj", "data/train", "container-1");

    verify(flowerDockerService)
        .copyDataToContainer("container-1", "/tmp/armadillo_data", "proj_data_train", data);
  }

  @Test
  void pushData_rejectsNonFlowerContainer() {
    when(containerService.getByName("default")).thenReturn(mock(DatashieldContainerConfig.class));

    assertThrows(
        IllegalArgumentException.class,
        () -> flowerDataService.pushData("myproject", "train.parquet", "default"));

    verify(flowerDockerService, never()).copyDataToContainer(any(), any(), any(), any());
  }
}
