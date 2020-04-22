package org.molgenis.datashield;

import static java.lang.String.format;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import java.net.URI;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

public class DataShieldUtils {

  public static final String TABLE_ENV = ".DSTableEnv";
  public static final String GLOBAL_ENV = ".GlobalEnv";

  private DataShieldUtils() {}

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

  static URI getLastCommandLocation() {
    return fromCurrentContextPath().replacePath("/lastcommand").build().toUri();
  }
}
