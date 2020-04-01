package org.molgenis.datashield;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.COMPLETED;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.exceptions.DataShieldSessionException;
import org.molgenis.datashield.pojo.DataShieldCommandDTO;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.r.RConnectionConsumer;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class DataShieldSessionTest {

  private DataShieldSession dataShieldSession;
  @Mock private RConnection rConnection;
  @Mock private RSession rSession;
  @Mock private RConnectionConsumer<REXP> rConnectionConsumer;
  @Mock private DataShieldConnectionFactory connectionFactory;
  @Mock private REXP rexp;
  @Mock private RExecutorService rExecutorService;
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  @BeforeEach
  public void before() {
    this.dataShieldSession =
        new DataShieldSession(connectionFactory, executorService, rExecutorService);
  }

  @Test
  void testGetRConnectionFromFactory() {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    assertSame(rConnection, dataShieldSession.getRConnection());
  }

  @Test
  void testGetRConnectionAttachToSession() throws RserveException {
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    dataShieldSession.tryDetachRConnection(rConnection);
    assertSame(rConnection, dataShieldSession.getRConnection());
  }

  @Test
  void testGetRConnectionAttachFails() throws RserveException {
    when(rConnection.detach()).thenReturn(rSession);
    doThrow(new RserveException(rConnection, "Cannot connect")).when(rSession).attach();

    dataShieldSession.tryDetachRConnection(rConnection);
    assertThrows(DataShieldSessionException.class, dataShieldSession::getRConnection);
  }

  @Test
  void testDetachFails() throws RserveException {
    doThrow(new RserveException(rConnection, "Not connected")).when(rConnection).detach();

    dataShieldSession.tryDetachRConnection(rConnection);
  }

  @Test
  void testSchedule() throws RserveException, ExecutionException, InterruptedException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);

    var result = dataShieldSession.schedule("ls()");

    assertSame(rexp, result.get());
    assertSame(result, dataShieldSession.getLastExecution());
  }

  @Test
  void getLastCommand() throws RserveException, ExecutionException, InterruptedException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);
    // schedule and await completion
    dataShieldSession.schedule("ls()").get();

    DataShieldCommandDTO command = dataShieldSession.getLastCommand().get();

    assertEquals("ls()", command.expression());
    assertEquals(COMPLETED, command.status());
  }

  @Test
  void testScheduleThrowingConsumer() throws REXPMismatchException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rExecutorService.execute("ls()", rConnection))
        .thenThrow(new RExecutionException(new REXPMismatchException(rexp, "access")));

    CompletableFuture<REXP> result = dataShieldSession.schedule("ls()");

    assertThrows(ExecutionException.class, result::get);
    verify(rConnection).detach();
  }

  @Test
  void execute() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);

    dataShieldSession.execute(rConnectionConsumer);

    assertAll(
        () -> verify(rConnectionConsumer).accept(rConnection), () -> verify(rConnection).detach());
  }

  @Test
  void executeFails() {
    RserveException cause = new RserveException(rConnection, "foutje");
    when(connectionFactory.createConnection())
        .thenThrow(new ConnectionCreationFailedException(cause));

    Assertions.assertThatThrownBy(() -> dataShieldSession.execute(rConnectionConsumer))
        .hasCause(cause);
  }

  @Test
  void sessionCleanup() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenReturn(rConnection);

    dataShieldSession.execute(rConnectionConsumer);
    dataShieldSession.sessionCleanup();

    assertAll(() -> verify(rSession).attach(), () -> verify(rConnection).close());
  }

  @Test
  void sessionCleanupFails() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rConnection.detach()).thenReturn(rSession);
    when(rSession.attach()).thenThrow(new RserveException(rConnection, "foutje"));

    dataShieldSession.execute(rConnectionConsumer);

    Assertions.assertThatThrownBy(() -> dataShieldSession.sessionCleanup())
        .hasCause(new RserveException(rConnection, "foutje"));
  }
}
