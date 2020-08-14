package org.molgenis.armadillo;

import static java.lang.String.format;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArmadilloUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloUtils.class);

  public static final String TABLE_ENV = ".DSTableEnv";
  public static final String GLOBAL_ENV = ".GlobalEnv";

  private ArmadilloUtils() {}

  public static String serializeExpression(String cmd) {
    return format("try(base::serialize({%s}, NULL))", cmd);
  }

  public static byte[] createRawResponse(REXP result) {
    try {
      byte[] rawResult = result.asBytes();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("RAW result: \n{}", hexDump(rawResult));
      }
      return rawResult;
    } catch (REXPMismatchException e) {
      throw new IllegalStateException(
          format("This was no 'RAW' result: [ %s ]", result.toDebugString()));
    }
  }

  static URI getLastCommandLocation() {
    return fromCurrentContextPath().replacePath("/lastcommand").build().toUri();
  }

  static String hexDump(byte[] bytes) {
    return hexDump(bytes, 16);
  }

  static String hexDump(byte[] bytes, int count) {
    var byteBuffer = ByteBuffer.wrap(bytes);
    Charset charset = Charset.forName("ISO-8859-1");
    var charBuffer = charset.decode(byteBuffer);
    byteBuffer.rewind();
    StringBuilder result = new StringBuilder();
    while (byteBuffer.hasRemaining()) {
      for (int colIndex = 0; colIndex < count; colIndex++) {
        if (byteBuffer.hasRemaining()) {
          result.append(String.format("%02x ", byteBuffer.get()));
        } else {
          result.append("   ");
        }
      }
      result.append(" |");
      for (int colIndex = 0; colIndex < count; colIndex++) {
        if (charBuffer.hasRemaining()) {
          char c = charBuffer.get();
          if (!Character.isISOControl(c)) {
            result.append(c);
          } else {
            result.append('.');
          }
        } else {
          result.append(".");
        }
      }
      result.append('|');
      if (byteBuffer.hasRemaining()) {
        result.append('\n');
      }
    }
    return result.toString();
  }
}
