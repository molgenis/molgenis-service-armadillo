package org.molgenis.armadillo;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.ArmadilloSessionException;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class ArmadilloSessionTest {

  private ArmadilloSession armadilloSession;
  @Mock private RConnection rConnection;
  @Mock private RSession rSession;
  @Mock private Function<RConnection, REXP> rConnectionConsumer;
  @Mock private ArmadilloConnectionFactory connectionFactory;
  @Mock private ProcessService processService;

  @BeforeEach
  void before() {
    this.armadilloSession = new ArmadilloSession(connectionFactory, processService);
  }

  @Test
  void testGetRConnectionFromFactory() {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    assertSame(rConnection, armadilloSession.getRConnection());
  }

  @Test
  void testGetRConnectionAttachToSession() throws RserveException {
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    armadilloSession.tryDetachRConnection(rConnection);
    assertSame(rConnection, armadilloSession.getRConnection());
  }

  @Test
  void testGetRConnectionAttachFails() throws RserveException {
    when(rConnection.detach()).thenReturn(rSession);
    doThrow(new RserveException(rConnection, "Cannot connect")).when(rSession).attach();

    armadilloSession.tryDetachRConnection(rConnection);
    assertThrows(ArmadilloSessionException.class, armadilloSession::getRConnection);
  }

  @Test
  void testDetachFails() throws RserveException {
    doThrow(new RserveException(rConnection, "Not connected")).when(rConnection).detach();

    armadilloSession.tryDetachRConnection(rConnection);
  }

  @Test
  void execute() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);

    armadilloSession.execute(rConnectionConsumer);

    assertAll(
        () -> verify(rConnectionConsumer).apply(rConnection), () -> verify(rConnection).detach());
  }

  @Test
  void executeFails() {
    RserveException cause = new RserveException(rConnection, "foutje");
    when(connectionFactory.createConnection())
        .thenThrow(new ConnectionCreationFailedException(cause));

    Assertions.assertThatThrownBy(() -> armadilloSession.execute(rConnectionConsumer))
        .hasCause(cause);
  }

  @Test
  void sessionCleanup() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    armadilloSession.execute(rConnectionConsumer);
    armadilloSession.sessionCleanup();

    assertAll(() -> verify(rSession).attach(), () -> verify(rConnection).close());
  }

  @Test
  void sessionCleanupTerminatesRunningProcess()
      throws RserveException, InterruptedException, ExecutionException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(processService.getPid(rConnection)).thenReturn(218);

    /*
     * This is a bit tricky since things have to happen in the correct order:
     * 1. Create connection and start the execution.</li>
     * 2. Destroy the session, trigger a termination of the r process.
     * So the execution has to wait until the session is destroyed before returning.
     * And the destroy has to wait till the execution is running before starting.
     */
    var executionIsRunning = new CountDownLatch(1);
    var sessionIsDestroyed = new CountDownLatch(1);
    var task =
        newSingleThreadExecutor()
            .submit(
                () -> {
                  armadilloSession.execute(
                      (connection) -> {
                        try {
                          executionIsRunning.countDown();
                          sessionIsDestroyed.await();
                        } catch (InterruptedException ignore) {
                        }
                        return new REXPNull();
                      });
                });
    executionIsRunning.await();
    armadilloSession.sessionCleanup();
    sessionIsDestroyed.countDown();
    task.get();

    verify(processService).terminateProcess(rConnection, 218);
  }

  @Test
  void sessionCleanupFails() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenThrow(new RserveException(rConnection, "foutje"));

    armadilloSession.execute(rConnectionConsumer);

    Assertions.assertThatThrownBy(() -> armadilloSession.sessionCleanup())
        .hasCause(new RserveException(rConnection, "foutje"));
  }
}
