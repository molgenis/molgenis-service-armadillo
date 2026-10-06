package org.molgenis.connection.flower;

import java.io.InputStream;
import org.molgenis.connection.flower.service.FlowerDockerService;

public class FlowerContainerConnection {
  // class is similar to RConnection for flower
  // TODO: prefix when running in container, do we do this here? Or
  //    String asContainerName(String containerName) {
  //        if (!inContainer) {
  //            LOG.warn("Image not running in docker container: " + containerName);
  //            return containerName;
  //        }
  //
  //        if (containerPrefix.isEmpty()) {
  //            LOG.error("Running in container without prefix: " + containerName);
  //            return containerName;
  //        }
  //
  //        LOG.warn("Image running in docker container: " + containerName);
  //        return containerPrefix + containerName + "-1";
  //    }
  private final String containerName;
  private FlowerDockerService dockerService;
  private static final String DATA_DIR = "/tmp/armadillo_data";

  public FlowerContainerConnection(String containerName, FlowerDockerService dockerService) {
    this.containerName = containerName;
    this.dockerService = dockerService;
  }

  public void pushData(String fileName, InputStream data) {
    long size = 123L;
    this.dockerService.copyDataToContainer(containerName, DATA_DIR, fileName, data, size);
  }
}
