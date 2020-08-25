package org.molgenis.armadillo;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.function.Predicate.not;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArmadilloUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloUtils.class);

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
    var charBuffer = ISO_8859_1.decode(byteBuffer);
    byteBuffer.rewind();
    StringBuilder result = new StringBuilder();
    while (byteBuffer.hasRemaining()) {
      for (int colIndex = 0; colIndex < count; colIndex++) {
        result.append(readHex(byteBuffer));
      }
      result.append(" |");
      for (int colIndex = 0; colIndex < count; colIndex++) {
        result.append(readAscii(charBuffer));
      }
      result.append('|');
      if (byteBuffer.hasRemaining()) {
        result.append('\n');
      }
    }
    return result.toString();
  }

  private static String readHex(ByteBuffer byteBuffer) {
    return byteBuffer.hasRemaining() ? String.format("%02x ", byteBuffer.get()) : "   ";
  }

  private static char readAscii(java.nio.CharBuffer charBuffer) {
    return charBuffer.hasRemaining()
        ? Optional.of(charBuffer.get()).filter(not(Character::isISOControl)).orElse('.')
        : '.';
  }
}
