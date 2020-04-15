package org.molgenis.datashield;

import static java.lang.String.format;

import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

public class DataShieldUtils {

  public static final String TABLE_ENV = ".DSTableEnv";
  public static final String GLOBAL_ENV = ".GlobalEnv";

  public static String serializeExpression(String cmd) {
    return format("try(base::serialize({%s}, NULL))", cmd);
  }

  public static byte[] createRawResponse(REXP result) {
    byte[] rawResult = new byte[0];
    if (result.isRaw()) {
      try {
        rawResult = result.asBytes();
      } catch (REXPMismatchException e) {
        throw new IllegalStateException(e);
      }
    }
    return rawResult;
  }

  public static Object asNativeJavaObject(REXP result) {
    try {
      return result.asNativeJavaObject();
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }
}
