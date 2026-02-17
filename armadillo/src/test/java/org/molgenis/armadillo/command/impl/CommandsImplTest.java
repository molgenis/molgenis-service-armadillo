package org.molgenis.armadillo.command.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.controller.ArmadilloUtils.GLOBAL_ENV;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.profile.ActiveProfileNameAccessor;
import org.molgenis.armadillo.security.ResourceTokenService;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerResult;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
class CommandsImplTest {

  @Mock ArmadilloStorageService armadilloStorage;
  @Mock PackageService packageService;
  @Mock RExecutorService rExecutorService;
  @Mock ProcessService processService;
  @Mock ProfileService profileService;
  @Mock ArmadilloConnectionFactory connectionFactory;
  @Mock RServerConnection rConnection;
  @Mock RequestAttributes attrs;
  @Mock ResourceTokenService resourceTokenService;
  @Mock InputStream inputStream;
  @Mock RServerResult rexp;
  @Mock Principal principal;

  static ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
  CommandsImpl commands;

  @BeforeAll
  static void beforeAll() {
    taskExecutor.initialize();
  }

  @BeforeEach
  void beforeEach() {
    JwtAuthenticationToken mockAuth = mock(JwtAuthenticationToken.class);
    lenient()
        .when(resourceTokenService.generateResourceToken(any(), any(), any()))
        .thenReturn(mockAuth);
    commands =
        new CommandsImpl(
            armadilloStorage,
            packageService,
            rExecutorService,
            taskExecutor,
            connectionFactory,
            processService,
            profileService,
            resourceTokenService);
  }

  @Test
  void testSchedule() throws Exception {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    ArmadilloCommandImpl<RServerResult> command =
        new ArmadilloCommandImpl<>("expression", true) {
          @Override
          protected RServerResult doWithConnection(RServerConnection connection) {
            assertSame(rConnection, connection);
            return rexp;
          }
        };
    CompletableFuture<RServerResult> result = commands.schedule(command);
    assertSame(rexp, result.get());
    assertEquals(Optional.of(command.asDto()), commands.getLastCommand());
    assertSame(result, commands.getLastExecution().get());
  }

  @Test
  void testScheduleFailingCommand() {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    IllegalStateException exception = new IllegalStateException("Error");

    ArmadilloCommandImpl<REXP> command =
        new ArmadilloCommandImpl<>("expression", true) {
          @Override
          protected REXP doWithConnection(RServerConnection connection) {
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
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    commands.assign("D", "E").get();

    verify(rExecutorService).execute("is.null(base::assign('D', value={E}))", rConnection);
  }

  @Test
  void testEvaluate() throws Exception {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    when(rExecutorService.execute("ls()", false, rConnection)).thenReturn(rexp);

    assertSame(rexp, commands.evaluate("ls()", false).get());
  }

  @Test
  void testSaveWorkspace() throws Exception {
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
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
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);

    commands.loadWorkspace(principal, "core").get();

    verify(rExecutorService)
        .loadWorkspace(eq(rConnection), any(InputStreamResource.class), eq(GLOBAL_ENV));
  }

  @Test
  void testLoadTable() throws Exception {
    when(armadilloStorage.loadTable("project", "folder/table")).thenReturn(inputStream);
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);

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
  void testInstallPackage() throws Exception {
    ArmadilloCommandImpl<REXP> command =
        new ArmadilloCommandImpl<>("Install package", false) {
          @Override
          protected REXP doWithConnection(RServerConnection connection) {
            verify(rExecutorService)
                .installPackage(eq(rConnection), any(Resource.class), any(String.class));
            return null;
          }
        };
  }

  @Test
  void testGetPackages() throws Exception {
    List<RPackage> result = Collections.emptyList();
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);
    when(packageService.getInstalledPackages(rConnection)).thenReturn(result);
    assertSame(result, commands.getPackages().get());
  }

  @Test
  void testCleanup() {
    commands.preDestroy();
  }

  @Test
  void testLoadResource() throws Exception {
    when(armadilloStorage.loadResource("gecko", "2_1-core-1_0/hpc-resource"))
        .thenReturn(inputStream);
    when(connectionFactory.createConnection()).thenReturn(rConnection);
    when(processService.getPid(rConnection)).thenReturn(218);

    commands.loadResource(principal, "core_nonrep", "gecko/2_1-core-1_0/hpc-resource").get();

    verify(rExecutorService)
        .loadResource(
            any(JwtAuthenticationToken.class),
            eq(rConnection),
            any(InputStreamResource.class),
            eq("gecko/2_1-core-1_0/hpc-resource.rds"),
            eq("core_nonrep"));
  }

  @Test
  void testGetActiveProfileDefault() {
    ActiveProfileNameAccessor.resetActiveProfileName();
    String profileName = commands.getActiveProfileName();
    assertEquals(ActiveProfileNameAccessor.DEFAULT, profileName);
  }

  @Test
  void testGetActiveProfile() {
    ActiveProfileNameAccessor.setActiveProfileName("exposome");
    String profileName = commands.getActiveProfileName();
    assertEquals("exposome", profileName);
    ActiveProfileNameAccessor.resetActiveProfileName();
  }

  @Test
  void testSelectProfileWritesToSession() {
    RequestContextHolder.setRequestAttributes(attrs);
    ProfileConfig profileConfig =
        ProfileConfig.create(
            "exposome",
            "dummy",
            false,
            null,
            "localhost",
            6311,
            Set.of(),
            Set.of(),
            Map.of(),
            null,
            null,
            null,
            null,
            null);
    when(profileService.getByName("exposome")).thenReturn(profileConfig);
    commands.selectProfile("exposome");
    verify(attrs).setAttribute("profile", "exposome", SCOPE_SESSION);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void testSelectUnknownProfile() {
    when(profileService.getByName("unknown")).thenThrow(new UnknownProfileException("unknown"));
    assertThrows(UnknownProfileException.class, () -> commands.selectProfile("unknown"));
  }

  @Test
  void testReadResource() throws Exception {
    String originalContent = "This is a test resource content";

    // Create real gzipped bytes
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
      gzipOut.write(originalContent.getBytes(StandardCharsets.UTF_8));
    }

    byte[] gzippedBytes = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(gzippedBytes);

    String result = commands.readResource(bais);

    assertEquals(originalContent, result);
  }

  @Test
  void testExtractResourceInfo() {
    String fileInfo = "some text /projects/myProject/objects/folder%2Ffile_name.rds more text";

    HashMap<String, String> result = commands.extractResourceInfo(fileInfo);

    assertEquals("myProject", result.get("project"));
    assertEquals("folder/file_name.rds", result.get("object"));
  }

  @Test
  void testExtractResourceInfoNoMatch() {
    String fileInfo = "no matching pattern here";

    HashMap<String, String> result = commands.extractResourceInfo(fileInfo);

    assertTrue(result.isEmpty());
  }
}
