package org.molgenis.armadillo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class ArmadilloSessionFactoryTest {

  ArmadilloSessionFactory armadilloSessionFactory;
  @Mock private ProcessService processService;
  @Mock private ArmadilloConnectionFactory armadilloConnectionFactory;
  @Mock private RConnection rConnection;

  @BeforeEach
  void setUp() {
    armadilloSessionFactory = new ArmadilloSessionFactory(processService);
  }

  @Test
  void createSession() {
    when(armadilloConnectionFactory.createConnection()).thenReturn(rConnection);

    var armadilloSession = armadilloSessionFactory.createSession(armadilloConnectionFactory);

    armadilloSession.sessionCleanup();
    verify(rConnection).close();
  }
}
