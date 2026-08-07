package org.molgenis.armadillo.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.molgenis.armadillo.storage.FileDownloader.getPercentage;
import static org.molgenis.armadillo.storage.FileDownloader.processFile;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class FileDownloaderTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void getPercentage_should_calculate_correctly() {
    assertThat(getPercentage(50, 100)).isEqualTo(50);
    assertThat(getPercentage(1, 100)).isEqualTo(1);
    assertThat(getPercentage(100, 100)).isEqualTo(100);
  }

  @Test
  void processFile_should_write_bytes_and_report_progress(@TempDir Path tempDir) throws Exception {
    byte[] data = "hello world".getBytes();
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data));
    File out = tempDir.resolve("out.bin").toFile();
    List<Long> progressUpdates = new ArrayList<>();

    try (FileOutputStream fos = new FileOutputStream(out)) {
      processFile(fos, in, data.length, progressUpdates::add);
    }

    assertThat(out).hasContent("hello world");
    assertThat(progressUpdates).isNotEmpty();
  }

  @Test
  void downloadFile_should_download_content_and_report_progress(@TempDir Path tempDir)
      throws Exception {
    byte[] content = "hello world, this is a test file".getBytes();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/file",
        exchange -> {
          exchange.sendResponseHeaders(200, content.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
          }
        });
    server.start();

    String url = "http://localhost:" + server.getAddress().getPort() + "/file";
    File outFile = tempDir.resolve("downloaded.bin").toFile();
    List<Long> progress = new ArrayList<>();

    FileDownloader.downloadFile(url, outFile.getAbsolutePath(), progress::add);

    assertThat(outFile).hasBinaryContent(content);
    assertThat(progress).isNotEmpty();
    assertThat(progress.get(progress.size() - 1)).isEqualTo(100L);
  }

  @Test
  void downloadFile_should_report_running_total_when_no_content_length(@TempDir Path tempDir)
      throws Exception {
    byte[] content = "no length header here".getBytes();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/file",
        exchange -> {
          // sendResponseHeaders(200, 0) forces chunked transfer, so no Content-Length header
          exchange.sendResponseHeaders(200, 0);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
          }
        });
    server.start();

    String url = "http://localhost:" + server.getAddress().getPort() + "/file";
    File outFile = tempDir.resolve("downloaded2.bin").toFile();
    List<Long> progress = new ArrayList<>();

    FileDownloader.downloadFile(url, outFile.getAbsolutePath(), progress::add);

    assertThat(outFile).hasBinaryContent(content);
    assertThat(progress).isNotEmpty();
    // without Content-Length, the callback receives cumulative bytes read, not a percentage
    assertThat(progress.get(progress.size() - 1)).isEqualTo((long) content.length);
  }

  @Test
  void downloadFile_should_throw_when_response_is_not_200(@TempDir Path tempDir) throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/missing",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
          exchange.close();
        });
    server.start();

    String url = "http://localhost:" + server.getAddress().getPort() + "/missing";
    File outFile = tempDir.resolve("shouldnotexist.bin").toFile();

    String outPath = outFile.getAbsolutePath();

    assertThatThrownBy(() -> FileDownloader.downloadFile(url, outPath))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatusCode.valueOf(404));

    assertThat(outFile).doesNotExist();
  }

  @Test
  void downloadFile_should_wrap_connection_failure_as_bad_request(@TempDir Path tempDir)
      throws Exception {
    // bind and immediately release a port so nothing is listening on it
    int freePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      freePort = socket.getLocalPort();
    }
    String url = "http://localhost:" + freePort + "/unreachable";
    File outFile = tempDir.resolve("nofile.bin").toFile();

    assertThatThrownBy(() -> FileDownloader.downloadFile(url, outFile.getAbsolutePath()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatusCode.valueOf(400));
  }
}
