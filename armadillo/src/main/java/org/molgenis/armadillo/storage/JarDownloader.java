package org.molgenis.armadillo.storage;

import java.io.IOException;
import java.util.function.LongConsumer;

public interface JarDownloader {
  void downloadFile(String url, String outputFile) throws InterruptedException, IOException;

  void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException, IOException;
}
