package org.molgenis.armadillo.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests that the Logback configuration includes log sanitization to prevent log injection attacks.
 * The pattern should use %replace to convert newlines to spaces.
 */
class LogSanitizationTest {

  @Test
  void logbackConfig_containsNewlineSanitizationPattern() throws IOException {

    try (InputStream is = getClass().getResourceAsStream("/logback-file.xml")) {
      assertNotNull(is, "logback-file.xml should exist on classpath");

      String config = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      assertTrue(
          config.contains("%replace(%msg)"),
          "logback-file.xml should use %replace to sanitize log messages. "
              + "This prevents log injection attacks where malicious input containing "
              + "newlines could forge fake log entries.");

      assertTrue(
          config.contains("\\r\\n|\\r|\\n") || config.contains("'\\r\\n|\\r|\\n'"),
          "logback-file.xml %replace pattern should target \\r\\n, \\r, and \\n");
    }
  }
}
