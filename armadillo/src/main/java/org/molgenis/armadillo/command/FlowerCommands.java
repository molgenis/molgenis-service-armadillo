package org.molgenis.armadillo.command;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.SHARED_PREFIX;

import java.io.InputStream;
import org.molgenis.armadillo.container.FlowerSuperExecContainerConfig;
import org.molgenis.armadillo.exceptions.NotFlowerSuperExecContainerException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.connection.flower.service.FlowerExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FlowerCommands {
  FlowerExecutorService flowerExecutorService;
  private final ContainerService containerService;
  private final String rootDir;

  public FlowerCommands(
      ContainerService containerService,
      @Value("${storage.root-dir}") String rootDir,
      @Autowired FlowerExecutorService flowerExecutorService) {
    this.rootDir = rootDir;
    this.flowerExecutorService = flowerExecutorService;
    this.containerService = containerService;
  }

  public void loadData(String project, String containerName, String object, InputStream data) {
    var config = runAsSystem(() -> containerService.getByName(containerName));
    if (!(config instanceof FlowerSuperExecContainerConfig)) {
      throw new NotFlowerSuperExecContainerException(containerName);
    }
    String fileName = String.format("%s/%s%s/%s", rootDir, SHARED_PREFIX, project, object);
    flowerExecutorService.loadData(containerName, fileName, data);
  }
}
