package org.molgenis.armadillo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.molgenis.armadillo.container.ContainerStatusService;
import org.molgenis.armadillo.metadata.ContainerStartStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "containers")
@RequestMapping("containers")
public class ContainerStartStatusController {
  private final ContainerStatusService statusService;

  public ContainerStartStatusController(ContainerStatusService statusService) {
    this.statusService = statusService;
  }

  @GetMapping("/{containerName}/status")
  public ContainerStartStatus getStatus(@PathVariable String containerName) {
    return statusService.getStatus(containerName);
  }
}
