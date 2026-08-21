package org.molgenis.armadillo.storage;

import java.io.IOException;
import java.util.function.LongConsumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface JarDownloader {
  void downloadFile(String url, String outputFile) throws InterruptedException, IOException;

  void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException, IOException;

  /** Downloads the given armadillo release version as a jar, reporting progress via SSE. */
  SseEmitter downloadArmadilloJar(String version);

  /** Verifies the locally installed jar's SHA-256 against the GitHub release digest. */
  Boolean isValidJar(String version) throws IOException, InterruptedException;
}
