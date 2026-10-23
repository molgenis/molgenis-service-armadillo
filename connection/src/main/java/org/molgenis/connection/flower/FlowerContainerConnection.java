package org.molgenis.connection.flower;

import java.io.InputStream;
import org.molgenis.connection.flower.service.FlowerDockerService;
import org.springframework.beans.factory.annotation.Autowired;

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
  private final FlowerDockerService dockerService;
  private static final String DATA_DIR = "/tmp/armadillo_data";

  public FlowerContainerConnection(
      String containerName, @Autowired FlowerDockerService dockerService) {
    this.containerName = containerName;
    this.dockerService = dockerService;
    // TODO figure out how to do this with spring, but over it for now
    //        this.dockerService = new FlowerDockerService();
  }

  public void pushData(String fileName, InputStream in, long size) {
    this.dockerService.copyDataToContainer(containerName, DATA_DIR, fileName, in, size);
    System.out.println("Writing!!!" + fileName);
  }
}
