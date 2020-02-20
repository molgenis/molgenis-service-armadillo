package org.molgenis.datashield.r;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RDatashieldSessionTest {

  private RDatashieldSession rDatashieldSession;
  @Mock private RConnection rConnection;
  @Mock private RSession rSession;
  @Mock private RConnectionConsumer rConnectionConsumer;
  @Mock private RConnectionFactory rConnectionFactory;

  @BeforeEach
  public void before() throws RserveException
  {
    this.rDatashieldSession = new RDatashieldSession(rConnectionFactory);

  }

  @SuppressWarnings("unchecked")
  @Test
  void execute() throws REXPMismatchException, RserveException {
    when(rConnectionFactory.getNewConnection(false)).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);

    rDatashieldSession.execute(rConnectionConsumer);

    assertAll(
        () -> verify(rConnectionConsumer).accept(rConnection), () -> verify(rConnection).detach());
  }

  @SuppressWarnings("unchecked")
  @Test
  void executeFails() throws REXPMismatchException, RserveException {
    when(rConnectionFactory.getNewConnection(false)).thenThrow(new RserveException(rConnection, "foutje"));

    Assertions.assertThatThrownBy(() -> rDatashieldSession.execute(rConnectionConsumer)).hasCause(new RserveException(rConnection, "foutje"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void sessionCleanup() throws REXPMismatchException, RserveException
  {
    when(rConnectionFactory.getNewConnection(false)).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    rDatashieldSession.execute(rConnectionConsumer);
    rDatashieldSession.sessionCleanup();

    assertAll(() -> verify(rSession).attach(),
    () -> verify(rConnection).close());
  }

  @SuppressWarnings("unchecked")
  @Test
  void sessionCleanupFails() throws REXPMismatchException, RserveException {
    when(rConnectionFactory.getNewConnection(false)).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenThrow(new RserveException(rConnection, "foutje"));

    rDatashieldSession.execute(rConnectionConsumer);

    Assertions.assertThatThrownBy(() -> rDatashieldSession.sessionCleanup()).hasCause(new RserveException(rConnection, "foutje"));
  }
}
