package org.molgenis.armadillo.storage;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.LongConsumer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public final class FileDownloader {
  private FileDownloader() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static void downloadFile(String url, String outputFile) throws InterruptedException {
    downloadFile(url, outputFile, progress -> {});
  }

  static long getPercentage(long total, long current) {
    return (total * 100) / current;
  }

  static void processFile(
      FileOutputStream fileOutputStream,
      BufferedInputStream in,
      long fileSize,
      LongConsumer progressCallback)
      throws IOException {
    byte[] dataBuffer = new byte[8192];
    int bytesRead;
    long totalRead = 0;
    long lastReportedPercent = -1;

    while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
      fileOutputStream.write(dataBuffer, 0, bytesRead);
      totalRead += bytesRead;

      if (fileSize > 0) {
        long percent = getPercentage(totalRead, fileSize);
        if (percent != lastReportedPercent) {
          lastReportedPercent = percent;
          progressCallback.accept(percent);
        }
      } else {
        progressCallback.accept(totalRead);
      }
    }
  }

  public static void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

      HttpResponse<InputStream> response =
          HttpClient.newBuilder()
              .proxy(ProxySelector.getDefault())
              .followRedirects(HttpClient.Redirect.NORMAL) // follow GitHub → S3 redirect
              .build()
              .send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() != 200) {
        throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()));
      }
      long fileSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
      try (BufferedInputStream in = new BufferedInputStream(response.body());
          FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
        processFile(fileOutputStream, in, fileSize, progressCallback);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
