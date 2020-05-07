package org.molgenis.armadillo;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.ArmadilloUtils.createRawResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPRaw;

@ExtendWith(MockitoExtension.class)
public class ArmadilloUtilsTest {

  @Mock REXP rexp;

  @Test
  public void testSerializeCommand() {
    String cmd = "meanDS(D$age";
    String serializedCommand = ArmadilloUtils.serializeExpression(cmd);
    assertEquals("try(base::serialize({meanDS(D$age}, NULL))", serializedCommand);
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
}
