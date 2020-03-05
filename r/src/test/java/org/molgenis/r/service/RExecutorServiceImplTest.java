package org.molgenis.r.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.molgenis.r.model.Column;
import org.molgenis.r.model.ColumnType;
import org.molgenis.r.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;

class RExecutorServiceImplTest {

  private RExecutorService executorService;

  @BeforeEach
  void before() {
    executorService = new RExecutorServiceImpl();
  }

  @Test
  void execute() throws RserveException, REXPMismatchException {
    RConnection rConnection = Mockito.mock(RConnection.class);
    REXP rexp = Mockito.mock(REXP.class);
    Mockito.when(rConnection.eval("mean(age)")).thenReturn(rexp);

    REXP result = executorService.execute("mean(age)", rConnection);

    Assertions.assertSame(rexp, result);
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
    Assertions.assertEquals(expected, RExecutorServiceImpl.getColType(columnType));
  }

  @Test
  void testColTypeFactor() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> RExecutorServiceImpl.getColType(ColumnType.FACTOR));
  }

  @Test
  void testGetColTypeLogical() {
    Assertions.assertEquals("col_logical()", RExecutorServiceImpl.getColType(ColumnType.LOGICAL));
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
    Mockito.when(rConnection.createFile("table_patients.csv")).thenReturn(rFileOutputStream);
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    Mockito.when(rConnection.eval(ArgumentMatchers.any())).thenReturn(Mockito.mock(REXP.class));

    executorService.assign(inputStream, table, rConnection);

    String expectedPackageEval =
        "if (!require(readr)) { install.packages('readr', repos=c('http://cran.r-project.org'), dependencies=TRUE) }";
    String expectedAssignEval =
        "base::is.null(base::assign('table_patients', readr::read_csv('table_patients.csv', "
            + "col_types = cols ( id = col_character(), name = col_character(), age = col_integer() ), "
            + "na = c(''))))";
    String expectedUnlinkEval = "base::unlink('table_patients.csv')";
    Assertions.assertAll(
        () -> Mockito.verify(rConnection).createFile("table_patients.csv"),
        () -> Mockito.verify(rFileOutputStream).close(),
        () -> Mockito.verify(rConnection).eval(expectedPackageEval),
        () -> Mockito.verify(rConnection).eval(expectedAssignEval),
        () -> Mockito.verify(rConnection).eval(expectedUnlinkEval));
  }
}
