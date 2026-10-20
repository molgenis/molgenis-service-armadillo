package org.molgenis.connection.flower;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
  private final FlowerDockerService dockerService;
  private static final String DATA_DIR = "/tmp/armadillo_data";

  public FlowerContainerConnection(String containerName) {
    this.containerName = containerName;
    // TODO figure out how to do this with spring, but over it for now
    this.dockerService = new FlowerDockerService();
  }

  public void pushData(String fileName, InputStream in) throws IOException {
    Path tmp = Files.createTempFile("flower-upload-", fileName + ".tmp");
    try {
      long size = Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
      try (InputStream data = new BufferedInputStream(Files.newInputStream(tmp))) {
        this.dockerService.copyDataToContainer(containerName, DATA_DIR, fileName, data, size);
      }
    } finally {
      Files.deleteIfExists(tmp);
    }
  }
}
