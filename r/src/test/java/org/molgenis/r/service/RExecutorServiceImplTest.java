package org.molgenis.r.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
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
import org.molgenis.r.rserve.RserveResult;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;

@ExtendWith(MockitoExtension.class)
class RExecutorServiceImplTest {

  private RExecutorServiceImpl executorService;
  @Mock private RServerConnection rConnection;
  @Mock private RServerResult rexp;
  @Mock private RFileOutputStream rFileOutputStream;
  @Mock private RFileInputStream rFileInputStream;

  @BeforeEach
  void before() {
    executorService = new RExecutorServiceImpl();
  }

  @Test
  void execute() throws RServerException {
    when(rConnection.eval("try({mean(age)})")).thenReturn(rexp);

    RServerResult result = executorService.execute("mean(age)", rConnection);

    assertSame(rexp, result);
  }

  @Test
  void executeFail() throws RServerException {
    when(rConnection.eval("try({mean(ages)})"))
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
    when(rConnection.eval("try({mean(age)})"))
        .thenThrow(new RExecutionException("Error in try(mean(age)) : object 'age' not found"));

    RExecutionException thrown =
        assertThrows(
            RExecutionException.class, () -> executorService.execute("mean(age)", rConnection));
    assertEquals("Error in try(mean(age)) : object 'age' not found", thrown.getMessage());
  }

  @Test
  void executeFailResultIsNull() throws RServerException {
    when(rConnection.eval("try({mean(child_id)})")).thenReturn(null);

    RExecutionException rExecutionException =
        assertThrows(
            RExecutionException.class,
            () -> executorService.execute("mean(child_id)", rConnection),
            "Eval returned null");
    assertTrue(rExecutionException.getMessage().contains("Eval returned null"));
  }

  @Test
  void testLoadWorkspace() throws IOException, RServerException {
    when(rConnection.createFile(".RData")).thenReturn(rFileOutputStream);
    Resource resource = new InMemoryResource("Hello");

    executorService.loadWorkspace(rConnection, resource, ".TibbleEnv");

    verify(rConnection).eval("base::load(file='.RData', envir=.TibbleEnv)");
    verify(rConnection).eval("base::unlink('.RData')");
  }

  @Test
  void testLoadTableWithVariables() throws IOException, RServerException {
    when(rConnection.createFile("project_folder_table.parquet")).thenReturn(rFileOutputStream);
    Resource resource = new InMemoryResource("Hello");

    when(rConnection.eval(
            "try({is.null(base::assign('D', value={arrow::read_parquet('project_folder_table.parquet', col_select = tidyselect::any_of(c(\"col1\",\"col2\")))}))})"))
        .thenReturn(new RserveResult(new REXPLogical(true)));
    when(rConnection.eval("try({base::unlink('project_folder_table.parquet')})"))
        .thenReturn(new RserveResult(new REXPNull()));

    executorService.loadTable(
        rConnection, resource, "project/folder/table.parquet", "D", List.of("col1", "col2"));

    verify(rConnection)
        .eval(
            "try({is.null(base::assign('D', value={arrow::read_parquet('project_folder_table.parquet', col_select = tidyselect::any_of(c(\"col1\",\"col2\")))}))})");
    verify(rConnection).eval("try({base::unlink('project_folder_table.parquet')})");
  }

  @Test
  void testLoadTableNoVariables() throws IOException, RServerException {
    when(rConnection.createFile("project_folder_table.parquet")).thenReturn(rFileOutputStream);
    Resource resource = new InMemoryResource("Hello");

    when(rConnection.eval(
            "try({is.null(base::assign('D', value={arrow::read_parquet('project_folder_table.parquet')}))})"))
        .thenReturn(new RserveResult(new REXPLogical(true)));
    when(rConnection.eval("try({base::unlink('project_folder_table.parquet')})"))
        .thenReturn(new RserveResult(new REXPNull()));

    executorService.loadTable(
        rConnection, resource, "project/folder/table.parquet", "D", List.of());

    verify(rConnection)
        .eval(
            "try({is.null(base::assign('D', value={arrow::read_parquet('project_folder_table.parquet')}))})");
    verify(rConnection).eval("try({base::unlink('project_folder_table.parquet')})");
  }

  @Test
  void testLoadResource() throws IOException, RServerException {
    when(rConnection.createFile("project_folder_resource.rds")).thenReturn(rFileOutputStream);
    Resource resource = new InMemoryResource("Hello");

    when(rConnection.eval(
            "try({is.null(base::assign('D', value={resourcer::newResourceClient(base::readRDS('project_folder_resource.rds'))}))})"))
        .thenReturn(new RserveResult(new REXPLogical(true)));
    when(rConnection.eval("try({base::unlink('project_folder_resource.rds')})"))
        .thenReturn(new RserveResult(new REXPNull()));

    executorService.loadResource(rConnection, resource, "project/folder/resource.rds", "D");

    verify(rConnection)
        .eval(
            "try({is.null(base::assign('D', value={resourcer::newResourceClient(base::readRDS('project_folder_resource.rds'))}))})");
    verify(rConnection).eval("try({base::unlink('project_folder_resource.rds')})");
  }

  @Test
  void testSaveWorkspace() throws IOException, RServerException {
    when(rConnection.eval("try({base::save.image()})"))
        .thenReturn(new RserveResult(new REXPNull()));
    when(rConnection.openFile(".RData")).thenReturn(rFileInputStream);

    executorService.saveWorkspace(
        rConnection, inputStream -> assertSame(rFileInputStream, inputStream));

    verify(rConnection).eval("try({base::save.image()})");
    verify(rConnection).openFile(".RData");
  }

  @Test
  void testSaveWorkspaceFails() throws IOException, RServerException {
    when(rConnection.eval("try({base::save.image()})"))
        .thenReturn(new RserveResult(new REXPNull()));
    when(rConnection.openFile(".RData")).thenThrow(IOException.class);

    assertThrows(
        RExecutionException.class,
        () ->
            executorService.saveWorkspace(
                rConnection, inputStream -> assertSame(rFileInputStream, inputStream)));
  }

  @Test
  void testLoadWorkspaceFails() throws IOException {
    when(rConnection.createFile(".RData")).thenThrow(IOException.class);
    Resource resource = new InMemoryResource("Hello");

    assertThrows(
        RExecutionException.class, () -> executorService.loadWorkspace(rConnection, resource, "D"));
  }

  @Test
  void testLoadResourceFails() throws IOException {
    when(rConnection.createFile("hpc-resource-1.rds")).thenThrow(IOException.class);
    Resource resource = new InMemoryResource("Hello");

    assertThrows(
        RExecutionException.class,
        () -> executorService.loadResource(rConnection, resource, "hpc-resource-1.rds", "D"));
  }

  @Test
  void testInstallPackageFails() throws IOException {
    Resource resource = new InMemoryResource("Hello");
    String fileName = "test.txt";

    assertThrows(
        InvalidRPackageException.class,
        () -> executorService.installPackage(rConnection, resource, fileName));
  }

  @Test
  void testInstallPackage() throws IOException, RServerException {
    when(rConnection.createFile("location__test_.tar.gz")).thenReturn(rFileOutputStream);
    when(rConnection.eval(
            "try({remotes::install_local('location__test_.tar.gz', dependencies = TRUE, upgrade = 'never')})"))
        .thenReturn(new RserveResult(new REXPNull()));
    when(rConnection.eval("try({require('location/_test')})"))
        .thenReturn(new RserveResult(new REXPLogical(true)));
    when(rConnection.eval("try({file.remove('location/_test_.tar.gz')})"))
        .thenReturn(new RserveResult(new REXPNull()));

    Resource resource = new InMemoryResource("Hello");
    String fileName = "location/_test_.tar.gz";

    executorService.installPackage(rConnection, resource, fileName);

    verify(rConnection)
        .eval(
            "try({remotes::install_local('location__test_.tar.gz', dependencies = TRUE, upgrade = 'never')})");
    verify(rConnection).eval("try({require('location/_test')})");
    verify(rConnection).eval("try({file.remove('location/_test_.tar.gz')})");
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
