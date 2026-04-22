package org.molgenis.armadillo.service;

import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import java.io.IOException;
import java.io.InputStream;
import org.molgenis.armadillo.container.FlowerDockerService;
import org.molgenis.armadillo.container.FlowerSuperexecContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
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
  private final ContainerService containerService;

  public FlowerDataService(
      ArmadilloStorageService storageService,
      FlowerDockerService flowerDockerService,
      ContainerService containerService) {
    this.storageService = storageService;
    this.flowerDockerService = flowerDockerService;
    this.containerService = containerService;
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public void pushData(String project, String resource, String containerName) {
    var config = containerService.getByName(containerName);
    if (!(config instanceof FlowerSuperexecContainerConfig)) {
      throw new IllegalArgumentException(
          "Container '" + containerName + "' is not a Flower clientapp container");
    }
    String fileName = project + "_" + resource.replace("/", "_");
    try (InputStream data = storageService.loadObject(project, resource)) {
      flowerDockerService.copyDataToContainer(containerName, DATA_DIR, fileName, data);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read data for " + project + "/" + resource, e);
    }
  }
}
