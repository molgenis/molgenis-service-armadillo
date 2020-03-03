package org.molgenis.r.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.model.Column;
import org.molgenis.r.model.ColumnType;
import org.molgenis.r.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;

@ExtendWith(MockitoExtension.class)
class RExecutorServiceImplTest {

  private RExecutorServiceImpl executorService;
  @Mock private RConnection rConnection;
  @Mock private REXP rexp;
  @Mock private RFileOutputStream rFileOutputStream;
  @Mock private RFileInputStream rFileInputStream;

  @BeforeEach
  void before() {
    executorService = new RExecutorServiceImpl();
  }

  @Test
  void execute() throws RserveException {
    when(rConnection.eval("mean(age)")).thenReturn(rexp);

    REXP result = executorService.execute("mean(age)", rConnection);

    Assertions.assertSame(rexp, result);
  }

  @Test
  public void testLoadWorkspace() throws IOException, RserveException {
    when(rConnection.createFile(".RData")).thenReturn(rFileOutputStream);
    Resource resource = new InMemoryResource("Hello");

    executorService.loadWorkspace(rConnection, resource);

    verify(rConnection).eval("base::load(file='.RData')");
  }

  @Test
  public void testSaveWorkspace() throws IOException, RserveException {
    when(rConnection.openFile(".RData")).thenReturn(rFileInputStream);

    executorService.saveWorkspace(
        rConnection, inputStream -> assertSame(rFileInputStream, inputStream));

    verify(rConnection).eval("base::save.image()");
  }

  @ParameterizedTest()
  @CsvSource({
    "LOGICAL,col_logical()",
    "INTEGER,col_integer()",
    "CHARACTER,col_character()",
    "DATE,col_date()",
    "DATE_TIME,col_datetime()",
    "DOUBLE,col_double()"
  })
  void testGetColType(ColumnType columnType, String expected) {
    assertEquals(expected, RExecutorServiceImpl.getColType(columnType));
  }

  @Test
  void testColTypeFactor() {
    assertThrows(
        IllegalArgumentException.class, () -> RExecutorServiceImpl.getColType(ColumnType.FACTOR));
  }

  @Test
  void testGetColTypeLogical() {
    assertEquals("col_logical()", RExecutorServiceImpl.getColType(ColumnType.LOGICAL));
  }

  @Test
  void assign() throws IOException, RserveException {
    Table table =
        Table.builder()
            .setName("table_patients")
            .addColumn(Column.builder().setName("id").setType(ColumnType.CHARACTER).build())
            .addColumn(Column.builder().setName("name").setType(ColumnType.CHARACTER).build())
            .addColumn(Column.builder().setName("age").setType(ColumnType.INTEGER).build())
            .build();
    RConnection rConnection = Mockito.mock(RConnection.class);
    RFileOutputStream rFileOutputStream = Mockito.mock(RFileOutputStream.class);
    when(rConnection.createFile("table_patients.csv")).thenReturn(rFileOutputStream);
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    Resource csv = new InputStreamResource(inputStream);
    when(rConnection.eval(ArgumentMatchers.any())).thenReturn(Mockito.mock(REXP.class));

    executorService.assign(csv, table, rConnection);

    String expectedPackageEval =
        "if (!require(readr)) { install.packages('readr', repos=c('http://cran.r-project.org'), dependencies=TRUE) }";
    String expectedAssignEval =
        "base::is.null(base::assign('table_patients', readr::read_csv('table_patients.csv', "
            + "col_types = cols ( id = col_character(), name = col_character(), age = col_integer() ), "
            + "na = c(''))))";
    String expectedUnlinkEval = "base::unlink('table_patients.csv')";
    Assertions.assertAll(
        () -> verify(rConnection).createFile("table_patients.csv"),
        () -> verify(rFileOutputStream).close(),
        () -> verify(rConnection).eval(expectedPackageEval),
        () -> verify(rConnection).eval(expectedAssignEval),
        () -> verify(rConnection).eval(expectedUnlinkEval));
  }
}
