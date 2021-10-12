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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.ArmadilloSession;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.profile.ActiveProfileNameAccessor;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
class CommandsImplTest {

  @Mock ArmadilloStorageService armadilloStorage;
  @Mock PackageService packageService;
  @Mock RExecutorService rExecutorService;
  @Mock ProcessService processService;
  @Mock DataShieldConfigProps dataShieldConfigProps;
  @Mock ArmadilloConnectionFactory connectionFactory;
  @Mock RConnection rConnection;
  @Mock RequestAttributes attrs;

  @Mock InputStream inputStream;
  @Mock REXP rexp;
  @Mock Principal principal;

  static ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
  CommandsImpl commands;

  @BeforeAll
  static void beforeAll() {
    taskExecutor.initialize();
  }

  @BeforeEach
  void beforeEach() {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    ArmadilloSession armadilloSession = new ArmadilloSession(connectionFactory, processService);
    commands =
        new CommandsImpl(
            armadilloStorage,
            packageService,
            rExecutorService,
            taskExecutor,
            armadilloSession,
            dataShieldConfigProps);
  }

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

  @Test
  void testGetActiveProfileDefault() {
    RequestContextHolder.setRequestAttributes(attrs);
    when(attrs.getSessionMutex()).thenReturn("mutex");
    String profileName = commands.getActiveProfileName();
    assertEquals(ActiveProfileNameAccessor.DEFAULT, profileName);
  }

  @Test
  void testGetActiveProfile() {
    RequestContextHolder.setRequestAttributes(attrs);
    when(attrs.getSessionMutex()).thenReturn("mutex");
    when(attrs.getAttribute("profile", RequestAttributes.SCOPE_SESSION)).thenReturn("exposome");
    String profileName = commands.getActiveProfileName();
    assertEquals("exposome", profileName);
  }

  @Test
  void testSelectProfile() {
    RequestContextHolder.setRequestAttributes(attrs);
    when(attrs.getSessionMutex()).thenReturn("mutex");
    ProfileConfigProps profileConfigProps = new ProfileConfigProps();
    profileConfigProps.setName("default");
    when(dataShieldConfigProps.getProfiles()).thenReturn(List.of(profileConfigProps));
    commands.selectProfile("default");
    verify(dataShieldConfigProps).getProfiles();
    verify(rConnection).close();
    assertEquals("default", ActiveProfileNameAccessor.getActiveProfileName());
  }

  @Test
  void testSelectUnknownProfile() {
    when(dataShieldConfigProps.getProfiles()).thenReturn(List.of());
    assertThrows(UnknownProfileException.class, () -> commands.selectProfile("unknown"));
  }
}
