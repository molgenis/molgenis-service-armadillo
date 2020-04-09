package org.molgenis.datashield;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.DataShieldUtils.asNativeJavaObject;
import static org.molgenis.datashield.DataShieldUtils.createRawResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.REXPString;

@ExtendWith(MockitoExtension.class)
public class DataShieldUtilsTest {

  @Mock REXP rexp;

  @Test
  public void testSerializeCommand() {
    String cmd = "meanDS(D$age";
    String serializedCommand = DataShieldUtils.serializeExpression(cmd);
    assertEquals("try(serialize({meanDS(D$age}, NULL))", serializedCommand);
  }

  @Test
  public void testCreateRawResponse() {
    byte[] bytes = {0x01, 0x02, 0x03};
    REXPRaw rexpDouble = new REXPRaw(bytes);
    byte[] actual = createRawResponse(rexpDouble);
    assertArrayEquals(bytes, actual);
  }

  @Test
  public void testCreateRawResponseNotRaw() {
    REXP rexp = new REXP();
    byte[] actual = createRawResponse(rexp);
    byte[] expected = new byte[0];
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testCreateRawResponseMisbehavingREXP() throws REXPMismatchException {
    when(rexp.isRaw()).thenReturn(true);
    doThrow(new REXPMismatchException(rexp, "blah")).when(rexp).asBytes();
    assertThrows(IllegalStateException.class, () -> createRawResponse(rexp));
  }

  @Test
  public void testAsNativeJavaObjectREXPNull() throws REXPMismatchException {
    assertEquals(null, asNativeJavaObject(new REXPNull()));
  }

  @Test
  public void testAsNativeJavaObjectREXPDouble() throws REXPMismatchException {
    Object actual = asNativeJavaObject(new REXPString("hello"));
    assertArrayEquals(new String[] {"hello"}, (String[]) actual);
  }

  @Test
  public void testAsNativeJavaObjectThrowsException() throws REXPMismatchException {
    doThrow(new REXPMismatchException(rexp, "blah")).when(rexp).asNativeJavaObject();
    assertThrows(RExecutionException.class, () -> asNativeJavaObject(rexp));
  }
}
