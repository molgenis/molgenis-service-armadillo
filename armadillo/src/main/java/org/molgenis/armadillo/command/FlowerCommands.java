package org.molgenis.armadillo.command;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.io.InputStream;
import org.molgenis.armadillo.container.FlowerSuperExecContainerConfig;
import org.molgenis.armadillo.exceptions.NotFlowerSuperExecContainerException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.connection.flower.service.FlowerExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlowerCommands {
  FlowerExecutorService flowerExecutorService;
  private final ContainerService containerService;

  public FlowerCommands(
      ContainerService containerService, @Autowired FlowerExecutorService flowerExecutorService) {
    this.flowerExecutorService = flowerExecutorService;
    this.containerService = containerService;
  }

  public void loadData(String project, String containerName, String object, InputStream data)
      throws Exception {
    var config = runAsSystem(() -> containerService.getByName(containerName));
    System.out.println(config);
    if (!(config instanceof FlowerSuperExecContainerConfig)) {
      throw new NotFlowerSuperExecContainerException(containerName);
    }
    // "%2F" (not "_") stands in for "/" so "data/train" and "data_train" can't collide.
    String fileName = project + "_" + object.replace("/", "%2F");
    System.out.println(fileName);
    flowerExecutorService.loadData(containerName, fileName, data);
  }
}
