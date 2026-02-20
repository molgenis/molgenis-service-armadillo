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
import org.molgenis.armadillo.container.ActiveContainerNameAccessor;
import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
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
  @Mock ContainerService containerService;
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
            containerService,
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
    String gzippedContent =
        "something /projects/gecko/objects/2_1-core-1_0%2Fhpc-resource.rds something";

    // Create real gzipped bytes
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
      gzipOut.write(gzippedContent.getBytes(StandardCharsets.UTF_8));
    }

    byte[] gzippedBytes = baos.toByteArray();

    // Print first few bytes for debugging
    System.out.println("GZIP byte data: " + Arrays.toString(gzippedBytes));

    ByteArrayInputStream bais = new ByteArrayInputStream(gzippedBytes);

    // Log the content read by GZIPInputStream
    String result = commands.readResource(bais);
    System.out.println("Decoded content: " + result);

    assertEquals(gzippedContent, result);
  }

  @Test
  void testGetActiveContainerDefault() {
    ActiveContainerNameAccessor.resetActiveContainerName();
    String containerName = commands.getActiveContainerName();
    assertEquals(ActiveContainerNameAccessor.DEFAULT, containerName);
  }

  @Test
  void testGetActiveContainer() {
    ActiveContainerNameAccessor.setActiveContainerName("exposome");
    String containerName = commands.getActiveContainerName();
    assertEquals("exposome", containerName);
    ActiveContainerNameAccessor.resetActiveContainerName();
  }

  @Test
  void testSelectContainerWritesToSession() {
    RequestContextHolder.setRequestAttributes(attrs);
    ContainerConfig containerConfig =
        ContainerConfig.create(
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
    when(containerService.getByName("exposome")).thenReturn(containerConfig);
    commands.selectContainer("exposome");
    verify(attrs).setAttribute("container", "exposome", SCOPE_SESSION);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void testSelectUnknownContainer() {
    when(containerService.getByName("unknown")).thenThrow(new UnknownContainerException("unknown"));
    assertThrows(UnknownContainerException.class, () -> commands.selectContainer("unknown"));
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
    String fileInfo =
        "X\u0000\u0000\u0000\u0003\u0000\u0004\u0005\u0000\u0000\u0003\u0005\u0000\u0000\u0000\u0000\u0005UTF-8\u0000\u0000\u0003\u0013\u0000\u0000\u0000\u0005\u0000\u0000\u0000\u0010\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000\u0004test\u0000\u0000\u0000\u0010\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000Uhttp://host.docker.internal:8080/storage/projects/omics/objects/ewas%2Fgse66351_1.rda\u0000\u0000\u0000�\u0000\u0000\u0000�\u0000\u0000\u0000\u0010\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000ExpressionSet\u0000\u0000\u0004\u0002\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000\u0005names\u0000\u0000\u0000\u0010\u0000\u0000\u0000\u0005\u0000\u0004\u0000\t\u0000\u0000\u0000\u0004name\u0000\u0004\u0000\t\u0000\u0000\u0000\u0003url\u0000\u0004\u0000\t\u0000\u0000\u0000\bidentity\u0000\u0004\u0000\t\u0000\u0000\u0000\u0006secret\u0000\u0004\u0000\t\u0000\u0000\u0000\u0006format\u0000\u0000\u0004\u0002\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000\u0005class\u0000\u0000\u0000\u0010\u0000\u0000\u0000\u0001\u0000\u0004\u0000\t\u0000\u0000\u0000\bresource\u0000\u0000\u0000�";

    HashMap<String, String> result = commands.extractResourceInfo(fileInfo);

    assertEquals("omics", result.get("project"));
    assertEquals("ewas/gse66351_1.rda", result.get("object"));
  }

  @Test
  void testExtractResourceInfoNoMatch() {
    String fileInfo = "no matching pattern here";

    HashMap<String, String> result = commands.extractResourceInfo(fileInfo);

    assertTrue(result.isEmpty());
  }
}
