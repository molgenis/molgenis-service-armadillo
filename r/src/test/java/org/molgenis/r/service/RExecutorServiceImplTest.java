package org.molgenis.r.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.InvalidRPackageException;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.rock.RockResult;
import org.molgenis.r.rock.RockServerException;
import org.molgenis.r.rserve.RserveResult;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.util.InMemoryResource;

@ExtendWith(MockitoExtension.class)
class RExecutorServiceImplTest {

  private RExecutorServiceImpl executorService;

  @Mock private RServerConnection rConnection;
  @Mock private RServerResult rexp;
  @Mock private RFileOutputStream rFileOutputStream;
  @Mock private RFileInputStream rFileInputStream;

  RExecutorServiceImplTest() {}

  @BeforeEach
  void before() {
    executorService = new RExecutorServiceImpl();
  }

  @Test
  void execute() throws RServerException {
    when(rConnection.eval("mean(age)", false)).thenReturn(rexp);

    RServerResult result = executorService.execute("mean(age)", rConnection);

    assertSame(rexp, result);
  }

  @Test
  void executeFail() throws RServerException {
    when(rConnection.eval("mean(ages)", false))
        .thenThrow(new RExecutionException(new Exception("Ages is not a valid column")));

    RExecutionException rExecutionException =
        assertThrows(
            RExecutionException.class,
            () -> executorService.execute("mean(ages)", rConnection),
            "Ages is not a valid column");
    assertTrue(rExecutionException.getMessage().contains("Ages is not a valid column"));
  }

  @Test
  void executeTryFails() throws RServerException {
    when(rConnection.eval("mean(age)", false))
        .thenThrow(new RExecutionException("Error in try(mean(age)) : object 'age' not found"));

    RExecutionException thrown =
        assertThrows(
            RExecutionException.class, () -> executorService.execute("mean(age)", rConnection));
    assertEquals("Error in try(mean(age)) : object 'age' not found", thrown.getMessage());
  }

  @Test
  void executeFailResultIsNull() throws RServerException {
    when(rConnection.eval("mean(child_id)", false)).thenReturn(null);

    RExecutionException rExecutionException =
        assertThrows(
            RExecutionException.class,
            () -> executorService.execute("mean(child_id)", rConnection),
            "Eval returned null");
    assertTrue(rExecutionException.getMessage().contains("Eval returned null"));
  }

  @Test
  void testLoadWorkspace() throws IOException, RServerException {
    Resource resource = new InMemoryResource("Hello");

    executorService.loadWorkspace(rConnection, resource, ".TibbleEnv");

    verify(rConnection).eval("base::load(file='.RData', envir=.TibbleEnv)");
    verify(rConnection).eval("base::unlink('.RData')");
  }

  @Test
  void testLoadTableWithVariables() throws IOException, RServerException {
    Resource resource = new InMemoryResource("Hello");

    when(rConnection.eval(
            "is.null(base::assign('D', value={data.frame(arrow::read_parquet('project_folder_table.parquet', as_data_frame = FALSE, col_select = tidyselect::any_of(c(\"col1\",\"col2\"))))}))",
            false))
        .thenReturn(new RockResult(new REXPLogical(true)));
    when(rConnection.eval("base::unlink('project_folder_table.parquet')", false))
        .thenReturn(new RockResult(new REXPNull()));

    executorService.loadTable(
        rConnection, resource, "project/folder/table.parquet", "D", List.of("col1", "col2"));

    verify(rConnection)
        .eval(
            "is.null(base::assign('D', value={data.frame(arrow::read_parquet('project_folder_table.parquet', as_data_frame = FALSE, col_select = tidyselect::any_of(c(\"col1\",\"col2\"))))}))",
            false);
    verify(rConnection).eval("base::unlink('project_folder_table.parquet')", false);
  }

  @Test
  void testLoadTableNoVariables() throws IOException, RServerException {
    Resource resource = new InMemoryResource("Hello");

    when(rConnection.eval(
            "is.null(base::assign('D', value={data.frame(arrow::read_parquet('project_folder_table.parquet', as_data_frame = FALSE))}))",
            false))
        .thenReturn(new RockResult(new REXPLogical(true)));
    when(rConnection.eval("base::unlink('project_folder_table.parquet')", false))
        .thenReturn(new RockResult(new REXPNull()));

    executorService.loadTable(
        rConnection, resource, "project/folder/table.parquet", "D", List.of());

    verify(rConnection)
        .eval(
            "is.null(base::assign('D', value={data.frame(arrow::read_parquet('project_folder_table.parquet', as_data_frame = FALSE))}))",
            false);
    verify(rConnection).eval("base::unlink('project_folder_table.parquet')", false);
  }

  @Test
  void testLoadResource() throws RServerException {
    var principal = mock(JwtAuthenticationToken.class, RETURNS_DEEP_STUBS);
    var token = mock(Jwt.class);
    Resource resource = new InMemoryResource("Hello");
    when(token.getTokenValue()).thenReturn("token");
    when(principal.getToken()).thenReturn(token);
    lenient()
        .doNothing()
        .when(rConnection)
        .writeFile(eq("project_folder_resource.rds"), any(InputStream.class));
    lenient()
        .doReturn(new RockResult(new REXPLogical(true)))
        .when(rConnection)
        .eval("is.null(base::assign('rds',base::readRDS('project_folder_resource.rds')))", false);
    lenient()
        .doReturn(new RockResult(new REXPNull()))
        .when(rConnection)
        .eval("base::unlink('project_folder_resource.rds')", false);
    lenient()
        .doReturn(new RockResult(new REXPLogical(true)))
        .when(rConnection)
        .eval(
            "is.null(base::assign('R', value={resourcer::newResource(\n"
                + "        name = rds$name,\n"
                + "        url = gsub('/objects/', '/rawfiles/', rds$url),\n"
                + "        format = rds$format,\n"
                + "        secret = \"token\"\n"
                + ")}))",
            false);
    lenient()
        .doReturn(new RockResult(new REXPLogical(true)))
        .when(rConnection)
        .eval("is.null(base::assign('D', value={resourcer::newResourceClient(R)}))", false);
    executorService.loadResource(
        principal, rConnection, resource, "project/folder/resource.rds", "D");
    verify(rConnection)
        .eval("is.null(base::assign('rds',base::readRDS('project_folder_resource.rds')))", false);
    verify(rConnection).eval("base::unlink('project_folder_resource.rds')", false);
    verify(rConnection)
        .eval(
            "is.null(base::assign('R', value={resourcer::newResource(\n"
                + "        name = rds$name,\n"
                + "        url = gsub('/objects/', '/rawfiles/', rds$url),\n"
                + "        format = rds$format,\n"
                + "        secret = \"token\"\n"
                + ")}))",
            false);
    verify(rConnection)
        .eval("is.null(base::assign('D', value={resourcer::newResourceClient(R)}))", false);
  }

  @Test
  void testSaveWorkspace() throws IOException, RServerException {
    when(rConnection.eval("base::save.image()", false)).thenReturn(new RockResult(new REXPNull()));

    executorService.saveWorkspace(
        rConnection, inputStream -> assertSame(rFileInputStream, inputStream));

    verify(rConnection).eval("base::save.image()", false);
  }

  @Test
  void testSaveWorkspaceFails() throws IOException, RServerException {
    when(rConnection.eval("base::save.image()", false)).thenReturn(new RockResult(new REXPNull()));
    doThrow(RockServerException.class).when(rConnection).readFile(anyString(), any(Consumer.class));

    assertThrows(
        RExecutionException.class,
        () ->
            executorService.saveWorkspace(
                rConnection, inputStream -> assertSame(rFileInputStream, inputStream)));
  }

  @Test
  void testLoadWorkspaceFails() throws RServerException {
    doThrow(RockServerException.class)
        .when(rConnection)
        .writeFile(anyString(), any(InputStream.class));
    Resource resource = new InMemoryResource("Hello");

    assertThrows(
        RExecutionException.class, () -> executorService.loadWorkspace(rConnection, resource, "D"));
  }

  @Test
  void testLoadResourceFails() {
    Resource resource = new InMemoryResource("Hello");
    var testPrincipal = mock(JwtAuthenticationToken.class);
    assertThrows(
        RExecutionException.class,
        () ->
            executorService.loadResource(
                testPrincipal, rConnection, resource, "hpc-resource-1.rds", "D"));
  }

  @Test
  void testInstallPackageFails() {
    Resource resource = new InMemoryResource("Hello");
    String fileName = "test.txt";

    assertThrows(
        InvalidRPackageException.class,
        () -> executorService.installPackage(rConnection, resource, fileName));
  }

  @Test
  void testInstallPackage() throws IOException, RServerException {
    when(rConnection.eval(
            "remotes::install_local('location__test_.tar.gz', dependencies = TRUE, upgrade = 'never')",
            false))
        .thenReturn(new RserveResult(new REXPNull()));
    when(rConnection.eval("require('location/_test')", false))
        .thenReturn(new RserveResult(new REXPLogical(true)));
    when(rConnection.eval("file.remove('location/_test_.tar.gz')", false))
        .thenReturn(new RserveResult(new REXPNull()));

    Resource resource = new InMemoryResource("Hello");
    String fileName = "location/_test_.tar.gz";

    executorService.installPackage(rConnection, resource, fileName);

    verify(rConnection)
        .eval(
            "remotes::install_local('location__test_.tar.gz', dependencies = TRUE, upgrade = 'never')",
            false);
    verify(rConnection).eval("require('location/_test')", false);
    verify(rConnection).eval("file.remove('location/_test_.tar.gz')", false);
  }

  @Test
  void testGetPackageNameFromFilename() {
    String filename = "hello_world_test.tar.gz";
    String pkgName = executorService.getPackageNameFromFilename(filename);
    assertEquals("hello_world", pkgName);
  }

  @Test
  void testGetRFilenameFromFilename() {
    String filename = "directory/test_file.tar.gz";
    String rFileName = executorService.getRFilenameFromFilename(filename);
    assertEquals("directory_test_file.tar.gz", rFileName);
  }
}
