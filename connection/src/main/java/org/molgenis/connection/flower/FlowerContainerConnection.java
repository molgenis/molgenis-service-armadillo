package org.molgenis.connection.flower;

import java.io.IOException;
import java.io.InputStream;
import org.molgenis.connection.exceptions.DataPushFailedException;
import org.molgenis.connection.flower.helpers.TarFile;
import org.molgenis.connection.flower.service.FlowerDockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class FlowerContainerConnection {
  /***
   * Flower equivalent for RockApplication (rock) and RConnection (rserve)
   * */
  private final String containerName;

  private final FlowerDockerService dockerService;
  private static final String DATA_DIR = "/tmp/armadillo_data";

  @Value("${armadillo.docker-run-in-container:false}")
  private boolean isArmadilloRunningInContainer;

  @Value("${armadillo.container-prefix:''}")
  private String containerPrefix;

  public FlowerContainerConnection(
      String containerName, @Autowired FlowerDockerService dockerService) {
    this.containerName = containerName;
    this.dockerService = dockerService;
  }

  public String getContainerName() {
    if (!isArmadilloRunningInContainer || containerPrefix.isEmpty()) {
      return containerName;
    } else {
      return containerPrefix + containerName + "-1";
    }
  }

  public void pushData(String fileName, InputStream in, long size) {
    try {
      TarFile tar = new TarFile(fileName, in, size);
      this.dockerService.copyCompressedDataToContainer(getContainerName(), DATA_DIR, tar);
    } catch (IOException e) {
      throw new DataPushFailedException(getContainerName(), e);
    }
  }
}
