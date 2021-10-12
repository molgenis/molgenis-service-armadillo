package org.molgenis.armadillo.command.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.controller.ArmadilloUtils.GLOBAL_ENV;

import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.ArmadilloSession;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { CommandsImpl.class, CommandsImplTest.Config.class })
@ExtendWith(MockitoExtension.class)
class CommandsImplTest {

  @MockBean ArmadilloStorageService armadilloStorage;
  @MockBean PackageService packageService;
  @MockBean RExecutorService rExecutorService;
  @MockBean ProcessService processService;
  @MockBean DataShieldConfigProps dataShieldConfigProps;
  @MockBean ArmadilloConnectionFactory connectionFactory;
  @MockBean RConnection rConnection;

  @Mock InputStream inputStream;
  @Mock REXP rexp;
  @Mock Principal principal;

  @Configuration
  public static class Config {

    @Bean
    public TaskExecutor taskExecutor() {
      return new ThreadPoolTaskExecutor();
    }

    /**
     *
     * Redefine ArmadilloSession as singleton
     * For test we do not need SessionScoped beans
     *
     * We need to mock the RConnection responses as a bean before constructing the ArmadilloSession in order to mock RConnection
     *
     */
    @Bean
    public ArmadilloSession armadilloSession(ArmadilloConnectionFactory connectionFactory, ProcessService processService, RConnection rConnection) {
      when(connectionFactory.createConnection()).thenReturn(rConnection);
      when(processService.getPid(rConnection)).thenReturn(218);
      return new ArmadilloSession(connectionFactory, processService);
    }
  }

  @Autowired
  private CommandsImpl commands;

  @Test
  void testSchedule() throws Exception {
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
  }

  @Test
  void testScheduleFailingCommand() throws RserveException {
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
  }

  @Test
  void testAssign() throws Exception {
    commands.assign("D", "E").get();

    verify(rExecutorService).execute("is.null(base::assign('D', value={E}))", rConnection);
  }

  @Test
  void testEvaluate() throws Exception {
    when(rExecutorService.execute("ls()", rConnection)).thenReturn(rexp);

    assertSame(rexp, commands.evaluate("ls()").get());
  }

  @Test
  void testSaveWorkspace() throws Exception {
    doAnswer(
            invocation -> {
              invocation.getArgument(1, Consumer.class).accept(inputStream);
              return null;
            })
        .when(rExecutorService)
        .saveWorkspace(eq(rConnection), any(Consumer.class));

    commands.saveWorkspace(principal, "core").get();

    verify(rExecutorService).saveWorkspace(eq(rConnection), any(Consumer.class));
  }

  @Test
  void testLoadWorkspace() throws Exception {
    when(armadilloStorage.loadWorkspace(principal, "core")).thenReturn(inputStream);

    commands.loadWorkspace(principal, "core").get();

    verify(rExecutorService)
        .loadWorkspace(eq(rConnection), any(InputStreamResource.class), eq(GLOBAL_ENV));
  }

  @Test
  void testLoadTable() throws Exception {
    when(armadilloStorage.loadTable("project", "folder/table")).thenReturn(inputStream);

    commands.loadTable("D", "project/folder/table", List.of("col1", "col2")).get();

    verify(rExecutorService)
        .loadTable(
            eq(rConnection),
            any(InputStreamResource.class),
            eq("project/folder/table.parquet"),
            eq("D"),
            eq(List.of("col1", "col2")));
  }

  @Test
  void testGetPackages() throws Exception {
    List<RPackage> result = Collections.emptyList();
    when(packageService.getInstalledPackages(rConnection)).thenReturn(result);
    assertSame(result, commands.getPackages().get());
  }

  @Test
  void testCleanup() {
    commands.preDestroy();

    verify(rConnection).close();
  }

  @Test
  void testLoadResource() throws Exception {
    when(armadilloStorage.loadResource("gecko", "2_1-core-1_0/hpc-resource"))
        .thenReturn(inputStream);

    commands.loadResource("core_nonrep", "gecko/2_1-core-1_0/hpc-resource").get();

    verify(rExecutorService)
        .loadResource(
            eq(rConnection),
            any(InputStreamResource.class),
            eq("gecko/2_1-core-1_0/hpc-resource.rds"),
            eq("core_nonrep"));
  }

  // TODO profile tests

}
