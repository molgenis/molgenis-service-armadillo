package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @Test
  void assign() throws IOException, RserveException {
    Table table = Table.builder().setName("table_patients").build();
    RConnection rConnection = mock(RConnection.class);
    RFileOutputStream rFileOutputStream = mock(RFileOutputStream.class);
    when(rConnection.createFile("table_patients.csv")).thenReturn(rFileOutputStream);
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    when(rConnection.eval(any())).thenReturn(mock(REXP.class));

    executorService.assign(inputStream, table, rConnection);

    String expectedPackageEval =
        "if (!require(readr)) { install.packages('readr', repos=c('http://cran.r-project.org'), dependencies=TRUE) }";
    String expectedAssignEval =
        "base::is.null(base::assign('table_patients', readr::read_csv('table_patients.csv')))";
    String expectedUnlinkEval = "base::unlink('table_patients.csv')";
    assertAll(
        () -> verify(rConnection).createFile("table_patients.csv"),
        () -> verify(rFileOutputStream).close(),
        () -> verify(rConnection).eval(expectedPackageEval),
        () -> verify(rConnection).eval(expectedAssignEval),
        () -> verify(rConnection).eval(expectedUnlinkEval));
  }
}
