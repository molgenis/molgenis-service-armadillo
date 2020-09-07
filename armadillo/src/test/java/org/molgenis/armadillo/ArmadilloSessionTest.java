package org.molgenis.armadillo;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class ArmadilloSessionTest {

  private ArmadilloSession armadilloSession;
  @Mock private RConnection rConnection;
  @Mock private Function<RConnection, REXP> rConnectionConsumer;
  @Mock private ArmadilloConnectionFactory connectionFactory;
  @Mock private ProcessService processService;

  @BeforeEach
  void before() {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    this.armadilloSession = new ArmadilloSession(connectionFactory, processService);
  }

  @Test
  void execute() {
    armadilloSession.execute(rConnectionConsumer);

    verify(rConnectionConsumer).apply(rConnection);
  }

  @Test
  void executeFails() {
    doThrow(new RExecutionException("foutje")).when(rConnectionConsumer).apply(rConnection);

    assertThrows(RExecutionException.class, () -> armadilloSession.execute(rConnectionConsumer));
  }

  @Test
  void sessionCleanup() {
    when(rConnection.close()).thenReturn(true);

    armadilloSession.sessionCleanup();

    verify(rConnection).close();
  }

  @Test
  void sessionCleanupTerminatesRunningProcess() throws InterruptedException, ExecutionException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);

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
}
