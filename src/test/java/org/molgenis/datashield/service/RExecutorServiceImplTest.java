package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.service.model.ColumnType.CHARACTER;
import static org.molgenis.datashield.service.model.ColumnType.FACTOR;
import static org.molgenis.datashield.service.model.ColumnType.INTEGER;
import static org.molgenis.datashield.service.model.ColumnType.LOGICAL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.molgenis.datashield.service.model.Column;
import org.molgenis.datashield.service.model.ColumnType;
import org.molgenis.datashield.service.model.Table;
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
    RConnection rConnection = mock(RConnection.class);
    REXP rexp = mock(REXP.class);
    when(rexp.asString()).thenReturn("36.6");
    when(rConnection.eval("mean(age)")).thenReturn(rexp);

    String result = executorService.execute("mean(age)", rConnection);

    assertEquals("36.6", result);
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
    assertThrows(IllegalArgumentException.class, () -> RExecutorServiceImpl.getColType(FACTOR));
  }

  @Test
  void testGetColTypeLogical() {
    assertEquals("col_logical()", RExecutorServiceImpl.getColType(LOGICAL));
  }

  @Test
  void assign() throws IOException, RserveException {
    Table table =
        Table.builder()
            .setName("table_patients")
            .addColumn(Column.builder().setName("id").setType(CHARACTER).build())
            .addColumn(Column.builder().setName("name").setType(CHARACTER).build())
            .addColumn(Column.builder().setName("age").setType(INTEGER).build())
            .build();
    RConnection rConnection = mock(RConnection.class);
    RFileOutputStream rFileOutputStream = mock(RFileOutputStream.class);
    when(rConnection.createFile("table_patients.csv")).thenReturn(rFileOutputStream);
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    when(rConnection.eval(any())).thenReturn(mock(REXP.class));

    executorService.assign(inputStream, table, rConnection);

    String expectedPackageEval =
        "if (!require(readr)) { install.packages('readr', repos=c('http://cran.r-project.org'), dependencies=TRUE) }";
    String expectedAssignEval =
        "base::is.null(base::assign('table_patients', readr::read_csv('table_patients.csv', "
            + "col_types = cols ( id = col_character(), name = col_character(), age = col_integer() ), "
            + "na = c(''))))";
    String expectedUnlinkEval = "base::unlink('table_patients.csv')";
    assertAll(
        () -> verify(rConnection).createFile("table_patients.csv"),
        () -> verify(rFileOutputStream).close(),
        () -> verify(rConnection).eval(expectedPackageEval),
        () -> verify(rConnection).eval(expectedAssignEval),
        () -> verify(rConnection).eval(expectedUnlinkEval));
  }
}
