package org.molgenis.armadillo;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

public class ArmadilloUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloUtils.class);

  public static final String TABLE_ENV = ".DSTableEnv";
  public static final String GLOBAL_ENV = ".GlobalEnv";

  private ArmadilloUtils() {}

  public static String serializeExpression(String cmd) {
    return format("try(base::serialize({%s}, NULL))", cmd);
  }

  public static byte[] createRawResponse(REXP result) {
    byte[] rawResult = new byte[0];
    if (result.isRaw()) {
      try {
        LOGGER.trace(format("RAW result : [ %s ]", new String(result.asBytes(), UTF_8)));
        rawResult = result.asBytes();
      } catch (REXPMismatchException e) {
        throw new IllegalStateException(e);
      }
    } else {
      try {
        LOGGER.debug(format("This was no 'RAW' result: [ %s ]", result.asNativeJavaObject()));
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
