package org.molgenis.armadillo.service;

import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {
  public ManagementService() {}

  public void restartApplictation() {
    ArmadilloServiceApplication.restart();
  }
}
