package org.molgenis.datashield.command.impl;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
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
import org.molgenis.datashield.model.Workspace;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.InputStreamResource;

@ExtendWith(MockitoExtension.class)
class CommandsImplTest {

  @Mock StorageService userStorageService;
  @Mock StorageService sharedStorageService;
  @Mock PackageService packageService;
  @Mock RExecutorService rExecutorService;
  @Mock DataShieldConnectionFactory connectionFactory;
  @Mock RConnection rConnection;
  @Mock InputStream inputStream;
  @Mock List<Workspace> workspaces;
  @Mock REXP rexp;
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private CommandsImpl commands;

  @BeforeEach
  public void beforeEach() {
    commands =
        new CommandsImpl(
            userStorageService,
            sharedStorageService,
            packageService,
            rExecutorService,
            executorService,
            connectionFactory);
  }

  @Test
  public void testSchedule() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    DataShieldCommandImpl<REXP> command =
        new DataShieldCommandImpl<>("expression", true) {
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
  public void testScheduleFailingCommand() throws RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    IllegalStateException exception = new IllegalStateException("Error");

    DataShieldCommandImpl<REXP> command =
        new DataShieldCommandImpl<>("expression", true) {
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
  public void testAssign() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    commands.assign("D", "E").get();

    verify(rExecutorService).execute("D <- E", rConnection);
    verify(rConnection).detach();
  }

  @Test
  public void testEvaluate() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);

    assertSame(rexp, commands.evaluate("ls()").get());
    verify(rConnection).detach();
  }

  @Test
  public void testLoadWorkspace() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(sharedStorageService.load("GECKO/core.RData")).thenReturn(inputStream);

    commands.loadWorkspaces(asList("GECKO/core.RData")).get();

    verify(rExecutorService)
        .loadWorkspace(eq(rConnection), any(InputStreamResource.class), eq(".DSTableEnv"));
    verify(rConnection).detach();
  }

  @Test
  public void testSaveWorkspace() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    doAnswer(
            invocation -> {
              invocation.getArgument(1, Consumer.class).accept(inputStream);
              return null;
            })
        .when(rExecutorService)
        .saveWorkspace(eq(rConnection), any(Consumer.class));

    commands.saveWorkspace("GECKO/core.RData").get();
    verify(rConnection).detach();
  }

  @Test
  public void testGetPackages() throws ExecutionException, InterruptedException, RserveException {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    List<RPackage> result = Collections.emptyList();
    when(packageService.getInstalledPackages(rConnection)).thenReturn(result);

    assertSame(result, commands.getPackages().get());
    verify(rConnection).detach();
  }

  @Test
  public void testListWorkspaces() {
    when(userStorageService.listWorkspaces("admin/")).thenReturn(workspaces);

    assertSame(workspaces, commands.listWorkspaces("admin/"));
  }

  @Test
  public void testDeleteWorkspace() {
    commands.removeWorkspace("admin/test.RData");

    verify(userStorageService).delete("admin/test.RData");
  }
}
