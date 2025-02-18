package org.molgenis.r.rserve;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class RserveConnectionTest {

  @InjectMocks private RserveConnection rserveConnection;
  @Mock private RConnection rConnection;
  @Mock private REXP rexp;

  RserveConnectionTest() {}

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rserveConnection = new RserveConnection(rConnection);
  }

  @Test
  void testEvalREXP() throws RServerException, RserveException {
    String exp = "mean(age)";
    RserveConnection conn = new RserveConnection(rConnection);
    when(rConnection.eval(exp)).thenReturn(rexp);
    REXP result = rserveConnection.evalREXP(exp);
    assertEquals(rexp, result);
  }

  @Test
  void testEvalREXPThrowsExceptionWhenEvalError() throws RserveException, REXPMismatchException {
    String exp = "mean(age)";
    String[] error = {"error 1 ", "error 2 "};
    when(rConnection.eval(exp)).thenReturn(rexp);
    when(rexp.inherits("try-error")).thenReturn(Boolean.TRUE);
    when(rexp.asStrings()).thenReturn(error);
    Exception exception =
        assertThrows(RExecutionException.class, () -> rserveConnection.evalREXP(exp));
    String expected = "error 1; error 2";
    assertTrue(exception.getMessage().contains(expected));
  }

  @Test
  void testEvalREXPThrowsExceptionWhenError() throws RserveException {
    String exp = "mean(age)";
    when(rConnection.eval(exp)).thenThrow(RserveException.class);
    assertThrows(org.molgenis.r.rserve.RserveException.class, () -> rserveConnection.evalREXP(exp));
  }

  @Test
  void testEvalREXPThrowsExceptionWhenMismatchError()
      throws RserveException, REXPMismatchException {
    String exp = "mean(age)";
    when(rConnection.eval(exp)).thenReturn(rexp);
    when(rexp.inherits("try-error")).thenReturn(Boolean.TRUE);
    when(rexp.asStrings()).thenThrow(REXPMismatchException.class);
    assertThrows(org.molgenis.r.rserve.RserveException.class, () -> rserveConnection.evalREXP(exp));
  }

  @Test
  void testReadFile() throws Exception {
    String fileName = ".RData";
    RFileInputStream mockInputStream = mock(RFileInputStream.class);
    Consumer<InputStream> mockConsumer = mock(Consumer.class);
    when(rConnection.openFile(fileName)).thenReturn(mockInputStream);
    rserveConnection.readFile(fileName, mockConsumer);
    verify(rConnection, times(1)).openFile(fileName);
    verify(mockConsumer, times(1)).accept(mockInputStream);
  }

  @Test
  void testReadFileThrowsIOException() throws Exception {
    String fileName = ".RData";
    Consumer<InputStream> mockConsumer = mock(Consumer.class);
    when(rConnection.openFile(fileName)).thenThrow(new IOException("Connection failed"));
    assertThrows(
        org.molgenis.r.rserve.RserveException.class,
        () -> rserveConnection.readFile(fileName, mockConsumer));
  }

  @Test
  void testEvalSerialized() throws Exception {
    String expression = "1 + 1";
    when(rConnection.eval(anyString())).thenReturn(rexp);
    RServerResult result = rserveConnection.eval(expression, true);

    assertNotNull(result);
    verify(rConnection, times(1)).eval(format("try(base::serialize({%s}, NULL))", expression));
  }

  @Test
  void testEvalNonSerialized() throws Exception {
    String expression = "2 + 2";
    when(rConnection.eval(anyString())).thenReturn(rexp);
    RServerResult result = rserveConnection.eval(expression, false);

    assertNotNull(result);
    verify(rConnection, times(1)).eval(format("try({%s})", expression));
  }
}
