package org.molgenis.r.rserve;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;

class RserveResultTest {

  @Mock private REXP mockREXP;

  private RserveResult rserveResult;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rserveResult = new RserveResult(mockREXP);
  }

  @Test
  void testLength() throws REXPMismatchException {
    // Simulate result.length()
    when(mockREXP.length()).thenReturn(10);
    int length = rserveResult.length();
    assertEquals(10, length);

    // Simulate an exception in length()
    when(mockREXP.length()).thenThrow(REXPMismatchException.class);
    length = rserveResult.length();
    assertEquals(-1, length);
  }

  @Test
  void testAsBytes() throws REXPMismatchException {
    byte[] expectedBytes = {1, 2, 3, 4};

    // Simulate result.isRaw() and result.asBytes()
    when(mockREXP.isRaw()).thenReturn(true);
    when(mockREXP.asBytes()).thenReturn(expectedBytes);
    byte[] actualBytes = rserveResult.asBytes();
    assertArrayEquals(expectedBytes, actualBytes);

    // If not raw, should return empty byte array
    when(mockREXP.isRaw()).thenReturn(false);
    actualBytes = rserveResult.asBytes();
    assertArrayEquals(new byte[0], actualBytes);
  }

  @Test
  void testIsNumeric() {
    when(mockREXP.isNumeric()).thenReturn(true);
    assertTrue(rserveResult.isNumeric());

    when(mockREXP.isNumeric()).thenReturn(false);
    assertFalse(rserveResult.isNumeric());
  }

  @Test
  void testAsDoubles() throws REXPMismatchException {
    double[] expectedDoubles = {1.1, 2.2, 3.3};

    // Simulate numeric REXP
    when(mockREXP.isNumeric()).thenReturn(true);
    when(mockREXP.asDoubles()).thenReturn(expectedDoubles);
    double[] actualDoubles = rserveResult.asDoubles();
    assertArrayEquals(expectedDoubles, actualDoubles);

    // Simulate a non-numeric case
    when(mockREXP.isNumeric()).thenReturn(false);
    actualDoubles = rserveResult.asDoubles();
    assertArrayEquals(new double[0], actualDoubles);
  }

  @Test
  void testIsInteger() {
    when(mockREXP.isInteger()).thenReturn(true);
    assertTrue(rserveResult.isInteger());

    when(mockREXP.isInteger()).thenReturn(false);
    assertFalse(rserveResult.isInteger());
  }

  @Test
  void testAsIntegers() throws REXPMismatchException {
    int[] expectedIntegers = {1, 2, 3};

    // Simulate integer REXP
    when(mockREXP.isInteger()).thenReturn(true);
    when(mockREXP.asIntegers()).thenReturn(expectedIntegers);
    int[] actualIntegers = rserveResult.asIntegers();
    assertArrayEquals(expectedIntegers, actualIntegers);

    // Simulate logical REXP
    when(mockREXP.isInteger()).thenReturn(false);
    when(mockREXP.isLogical()).thenReturn(true);
    when(mockREXP.asIntegers()).thenReturn(expectedIntegers);
    actualIntegers = rserveResult.asIntegers();
    assertArrayEquals(expectedIntegers, actualIntegers);

    // Non-integer and non-logical should return empty array
    when(mockREXP.isInteger()).thenReturn(false);
    when(mockREXP.isLogical()).thenReturn(false);
    actualIntegers = rserveResult.asIntegers();
    assertArrayEquals(new int[0], actualIntegers);
  }

  @Test
  void testAsInteger() throws REXPMismatchException {
    // Simulate result.asIntegers() returning a valid integer
    int[] ints = {42};
    when(mockREXP.isInteger()).thenReturn(true);
    when(mockREXP.asIntegers()).thenReturn(ints);
    assertEquals(42, rserveResult.asInteger());

    // Simulate an empty integer array
    ints = new int[0];
    when(mockREXP.asIntegers()).thenReturn(ints);
    assertThrows(RExecutionException.class, () -> rserveResult.asInteger());
  }

  @Test
  void testIsLogical() {
    when(mockREXP.isLogical()).thenReturn(true);
    assertTrue(rserveResult.isLogical());

    when(mockREXP.isLogical()).thenReturn(false);
    assertFalse(rserveResult.isLogical());
  }

  @Test
  void testIsNull() {
    when(mockREXP.isNull()).thenReturn(true);
    assertTrue(rserveResult.isNull());

    when(mockREXP.isNull()).thenReturn(false);
    assertFalse(rserveResult.isNull());
  }

  @Test
  void testIsString() {
    when(mockREXP.isString()).thenReturn(true);
    assertTrue(rserveResult.isString());

    when(mockREXP.isString()).thenReturn(false);
    assertFalse(rserveResult.isString());
  }

  @Test
  void testAsStrings() throws REXPMismatchException {
    String[] expectedStrings = {"hello", "world"};
    when(mockREXP.asStrings()).thenReturn(expectedStrings);
    String[] actualStrings = rserveResult.asStrings();
    assertArrayEquals(expectedStrings, actualStrings);
  }

  @Test
  void testIsList() {
    when(mockREXP.isList()).thenReturn(true);
    assertTrue(rserveResult.isList());

    when(mockREXP.isList()).thenReturn(false);
    assertFalse(rserveResult.isList());
  }

  @Test
  void testAsList() throws REXPMismatchException {
    RList expectedList = new RList();
    when(mockREXP.isList()).thenReturn(true);
    when(mockREXP.asList()).thenReturn(expectedList);
    List<RServerResult> actualList = rserveResult.asList();
    assertEquals(expectedList, actualList);
  }

  @Test
  void testIsNamedList() throws REXPMismatchException {
    RList rList = mock(RList.class);
    when(mockREXP.isList()).thenReturn(true);
    // Simulate named list
    when(mockREXP.asList()).thenReturn(rList);
    when(rList.isNamed()).thenReturn(true);
    assertTrue(rserveResult.isNamedList());

    // Simulate non-named list
    when(rList.isNamed()).thenReturn(false);
    assertFalse(rserveResult.isNamedList());
  }
}
