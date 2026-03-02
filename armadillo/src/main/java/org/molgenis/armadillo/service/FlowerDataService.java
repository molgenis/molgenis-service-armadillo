package org.molgenis.armadillo.service;

import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import java.io.InputStream;
import org.molgenis.armadillo.container.FlowerDockerService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class FlowerDataService {

  private static final String DATA_DIR = "/tmp/armadillo_data";

  private final ArmadilloStorageService storageService;
  private final FlowerDockerService flowerDockerService;

  public FlowerDataService(
      ArmadilloStorageService storageService, FlowerDockerService flowerDockerService) {
    this.storageService = storageService;
    this.flowerDockerService = flowerDockerService;
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public void pushData(String project, String resource, String containerName) {
    InputStream data = storageService.loadObject(project, resource);
    String fileName = project + "_" + resource.replace("/", "_");
    flowerDockerService.copyDataToContainer(containerName, DATA_DIR, fileName, data);
  }
}
