package org.molgenis.armadillo.service;

import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import java.io.InputStream;
import org.molgenis.armadillo.command.FlowerCommands;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class FlowerDataService {
  private final ArmadilloStorageService storageService;
  private final FlowerCommands flowerCommands;

  public FlowerDataService(ArmadilloStorageService storageService, FlowerCommands flowerCommands) {
    this.storageService = storageService;
    this.flowerCommands = flowerCommands;
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public InputStream loadData(String project, String resource) {
    try (InputStream data = storageService.loadObject(project, resource)) {
      return data;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load data for " + project + "/" + resource, e);
    }
  }
}
