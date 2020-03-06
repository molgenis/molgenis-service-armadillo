package org.molgenis.datashield;

import static java.lang.String.format;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

public class DataShieldUtils {
  public static String serializeCommand(String cmd) {
    return format("try(serialize({%s}, NULL))", cmd);
  }

  public static byte[] createRawResponse(REXP result) throws REXPMismatchException {
    byte[] rawResult = new byte[0];
    if (result.isRaw()) {
      rawResult = result.asBytes();
    }
    return rawResult;
  }
}
