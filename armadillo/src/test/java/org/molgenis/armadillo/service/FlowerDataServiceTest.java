package org.molgenis.armadillo.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.container.FlowerDockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;

@ExtendWith(MockitoExtension.class)
class FlowerDataServiceTest {

  @Mock ArmadilloStorageService storageService;
  @Mock FlowerDockerService flowerDockerService;

  private FlowerDataService flowerDataService;

  @BeforeEach
  void setup() {
    flowerDataService = new FlowerDataService(storageService, flowerDockerService);
  }

  @Test
  void pushData_success() {
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
    InputStream data = new ByteArrayInputStream("content".getBytes());
    when(storageService.loadObject("proj", "data/train")).thenReturn(data);

    flowerDataService.pushData("proj", "data/train", "container-1");

    verify(flowerDockerService)
        .copyDataToContainer("container-1", "/tmp/armadillo_data", "proj_data_train", data);
  }
}
