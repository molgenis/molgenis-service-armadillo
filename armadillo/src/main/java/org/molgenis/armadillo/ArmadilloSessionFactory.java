package org.molgenis.armadillo;

import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.service.ProcessService;
import org.springframework.stereotype.Component;

@Component
public class ArmadilloSessionFactory {
  private final ProcessService processService;

  public ArmadilloSessionFactory(ProcessService processService) {
    this.processService = processService;
  }

  public ArmadilloSession createSession(ArmadilloConnectionFactory armadilloConnectionFactory) {
    return new ArmadilloSession(armadilloConnectionFactory, processService);
  }
}
