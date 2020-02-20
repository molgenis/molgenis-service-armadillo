package org.molgenis.datashield.r;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RDatashieldSessionTest {

  @MockBean private RConnectionFactory rConnectionFactory;

  private RDatashieldSession rDatashieldSession;
  @Mock private RConnection rConnection;
  @Mock private RSession rSession;
  @Mock private RConnectionConsumer rConnectionConsumer;

  @BeforeEach
  public void before() throws RserveException {
    this.rDatashieldSession = new RDatashieldSession(rConnectionFactory);
    when(rConnectionFactory.getNewConnection(false)).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);
  }

  @Test
  void execute() throws REXPMismatchException, RserveException {
    rDatashieldSession.execute(rConnectionConsumer);
    verify(rConnectionConsumer.accept(rConnection));
  }

  @Test
  void sessionCleanup() {}
}
