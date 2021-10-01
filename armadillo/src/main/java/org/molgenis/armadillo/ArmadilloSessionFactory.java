package org.molgenis.armadillo;

import org.molgenis.armadillo.profile.Profile;
import org.molgenis.r.service.ProcessService;
import org.springframework.stereotype.Component;

@Component
public class ArmadilloSessionFactory {
  private final ProcessService processService;

  public ArmadilloSessionFactory(ProcessService processService) {
    this.processService = processService;
  }

  public ArmadilloSession createSession(Profile profile) {
    return new ArmadilloSession(
        profile.getProfileName(), profile.getArmadilloConnectionFactory(), processService);
  }
}
