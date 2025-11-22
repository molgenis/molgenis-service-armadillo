package org.molgenis.armadillo.controller;

import org.molgenis.armadillo.metadata.ContainerStartStatus;
import org.molgenis.armadillo.profile.ProfileStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ds-profiles")
public class ProfileStartStatusController {
  private final ProfileStatusService statusService;

  public ProfileStartStatusController(ProfileStatusService statusService) {
    this.statusService = statusService;
  }

  @GetMapping("/{profileName}/status")
  public ContainerStartStatus getStatus(@PathVariable String profileName) {
    return statusService.getStatus(profileName);
  }
}
