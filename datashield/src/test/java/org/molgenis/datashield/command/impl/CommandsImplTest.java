package org.molgenis.datashield.command.impl;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

  @Mock StorageService storageService;
  @Mock PackageService packageService;
  @Mock RExecutorService rExecutorService;
  @Mock DataShieldConnectionFactory connectionFactory;
  @Mock RConnection rConnection;
  @Mock InputStream inputStream;
  @Mock REXP rexp;
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private CommandsImpl commands;

  @BeforeEach
  public void beforeEach() {
    commands =
        new CommandsImpl(
            storageService, packageService, rExecutorService, executorService, connectionFactory);
    when(connectionFactory.createConnection()).thenReturn(rConnection);
  }

  @AfterEach
  public void afterEach() throws RserveException {
    verify(rConnection).detach();
  }

  @Test
  public void testSchedule() throws ExecutionException, InterruptedException {
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
  }

  @Test
  public void testScheduleFailingCommand() {
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
  }

  @Test
  public void testAssign() throws ExecutionException, InterruptedException {
    commands.assign("D", "E").get();

    verify(rExecutorService).execute("D <- E", rConnection);
  }

  @Test
  public void testEvaluate() throws ExecutionException, InterruptedException {
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);

    assertSame(rexp, commands.evaluate("ls()").get());
  }

  @Test
  public void testLoadWorkspace() throws ExecutionException, InterruptedException {
    when(storageService.load("GECKO/core.RData")).thenReturn(inputStream);

    commands.loadWorkspace("GECKO/core.RData", ".DSTableEnv").get();

    verify(rExecutorService)
        .loadWorkspace(eq(rConnection), any(InputStreamResource.class), eq(".DSTableEnv"));
  }

  @Test
  public void testSaveWorkspace() throws ExecutionException, InterruptedException {
    doAnswer(
            invocation -> {
              invocation.getArgument(1, Consumer.class).accept(inputStream);
              return null;
            })
        .when(rExecutorService)
        .saveWorkspace(eq(rConnection), any(Consumer.class));

    commands.saveWorkspace("GECKO/core.RData").get();
  }

  @Test
  public void testGetPackages() throws ExecutionException, InterruptedException {
    List<RPackage> result = Collections.emptyList();
    when(packageService.getInstalledPackages(rConnection)).thenReturn(result);

    assertSame(result, commands.getPackages().get());
  }
}
