package org.molgenis.armadillo.command.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.ArmadilloUtils.GLOBAL_ENV;

import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.InputStreamResource;

@ExtendWith(MockitoExtension.class)
class CommandsImplTest {

  @Mock ArmadilloStorageService armadilloStorage;
  @Mock PackageService packageService;
  @Mock RExecutorService rExecutorService;
  @Mock ArmadilloConnectionFactory connectionFactory;
  @Mock RConnection rConnection;
  @Mock InputStream inputStream;
  @Mock ProcessService processService;
  @Mock REXP rexp;
  @Mock Principal principal;
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private CommandsImpl commands;

  @BeforeEach
  void beforeEach() {
    commands =
        new CommandsImpl(
            armadilloStorage,
            packageService,
            rExecutorService,
            executorService,
            connectionFactory,
            processService);
  }

  @Test
  void testSchedule() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    ArmadilloCommandImpl<REXP> command =
        new ArmadilloCommandImpl<>("expression", true) {
          @Override
          protected REXP doWithConnection(RConnection connection) {
            assertSame(rConnection, connection);
            return rexp;
          }
        };
    CompletableFuture<REXP> result = commands.schedule(command);
    assertSame(rexp, result.get());
    assertEquals(Optional.of(command.asDto()), commands.getLastCommand());
    assertSame(result, commands.getLastExecution().get());
    verify(rConnection).detach();
  }

  @Test
  void testScheduleFailingCommand() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    IllegalStateException exception = new IllegalStateException("Error");

    ArmadilloCommandImpl<REXP> command =
        new ArmadilloCommandImpl<>("expression", true) {
          @Override
          protected REXP doWithConnection(RConnection connection) {
            assertSame(rConnection, connection);
            throw exception;
          }
        };
    CompletableFuture<REXP> result = commands.schedule(command);
    assertSame(
        exception, assertThrows(ExecutionException.class, result::get).getCause().getCause());
    verify(rConnection).detach();
  }

  @Test
  void testAssign() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    commands.assign("D", "E").get();

    verify(rExecutorService).execute("is.null(base::assign('D', value={E}))", rConnection);
    verify(rConnection).detach();
  }

  @Test
  void testEvaluate() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);

    assertSame(rexp, commands.evaluate("ls()").get());
    verify(rConnection).detach();
  }

  @Test
  void testSaveWorkspace() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    doAnswer(
            invocation -> {
              invocation.getArgument(1, Consumer.class).accept(inputStream);
              return null;
            })
        .when(rExecutorService)
        .saveWorkspace(eq(rConnection), any(Consumer.class));

    commands.saveWorkspace(principal, "core").get();
    verify(rConnection).detach();
  }

  @Test
  void testLoadWorkspace() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(armadilloStorage.loadWorkspace(principal, "core")).thenReturn(inputStream);

    commands.loadWorkspace(principal, "core").get();

    verify(rConnection).detach();
    verify(rExecutorService)
        .loadWorkspace(eq(rConnection), any(InputStreamResource.class), eq(GLOBAL_ENV));
  }

  @Test
  void testLoadTable() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(armadilloStorage.loadTable("project", "folder/table")).thenReturn(inputStream);

    commands.loadTable("D", "project/folder/table", List.of("col1", "col2")).get();

    verify(rConnection).detach();
    verify(rExecutorService)
        .loadTable(
            eq(rConnection),
            any(InputStreamResource.class),
            eq("project/folder/table.parquet"),
            eq("D"),
            eq(List.of("col1", "col2")));
  }

  @Test
  void testGetPackages() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    List<RPackage> result = Collections.emptyList();
    when(packageService.getInstalledPackages(rConnection)).thenReturn(result);

    assertSame(result, commands.getPackages().get());
    verify(rConnection).detach();
  }
}
