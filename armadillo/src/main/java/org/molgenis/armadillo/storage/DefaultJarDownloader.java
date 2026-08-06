package org.molgenis.armadillo.storage;

import java.util.function.LongConsumer;
import org.springframework.stereotype.Component;

@Component
public class DefaultJarDownloader implements JarDownloader {
  @Override
  public void downloadFile(String url, String outputFile) throws InterruptedException {
    FileDownloader.downloadFile(url, outputFile);
  }

  @Override
  public void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException {
    FileDownloader.downloadFile(url, outputFile, progressCallback);
  }
}
