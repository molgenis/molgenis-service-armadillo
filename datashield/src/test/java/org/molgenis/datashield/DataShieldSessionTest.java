package org.molgenis.datashield;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.r.RConnectionConsumer;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class DataShieldSessionTest {

  private DataShieldSession rDatashieldSession;
  @Mock private RConnection rConnection;
  @Mock private RSession rSession;
  @Mock private RConnectionConsumer rConnectionConsumer;
  @Mock private DataShieldConnectionFactory connectionFactory;

  @BeforeEach
  public void before() {
    this.rDatashieldSession = new DataShieldSession(connectionFactory);
  }

  @SuppressWarnings("unchecked")
  @Test
  void execute() throws REXPMismatchException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);

    rDatashieldSession.execute(rConnectionConsumer);

    assertAll(
        () -> verify(rConnectionConsumer).accept(rConnection), () -> verify(rConnection).detach());
  }

  @SuppressWarnings("unchecked")
  @Test
  void executeFails() {
    RserveException cause = new RserveException(rConnection, "foutje");
    when(connectionFactory.createConnection())
        .thenThrow(new ConnectionCreationFailedException(cause));

    Assertions.assertThatThrownBy(() -> rDatashieldSession.execute(rConnectionConsumer))
        .hasCause(cause);
  }

  @SuppressWarnings("unchecked")
  @Test
  void sessionCleanup() throws REXPMismatchException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    rDatashieldSession.execute(rConnectionConsumer);
    rDatashieldSession.sessionCleanup();

    assertAll(() -> verify(rSession).attach(), () -> verify(rConnection).close());
  }

  @SuppressWarnings("unchecked")
  @Test
  void sessionCleanupFails() throws REXPMismatchException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenThrow(new RserveException(rConnection, "foutje"));

    rDatashieldSession.execute(rConnectionConsumer);

    Assertions.assertThatThrownBy(() -> rDatashieldSession.sessionCleanup())
        .hasCause(new RserveException(rConnection, "foutje"));
  }
}
