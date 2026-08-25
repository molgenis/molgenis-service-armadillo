package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.molgenis.armadillo.storage.FileDownloader;
import org.springframework.web.server.ResponseStatusException;

class UpdateScriptDownloaderTest {

  @TempDir Path tempDir;

  UpdateScriptDownloader downloader;

  @BeforeEach
  void setUp() {
    downloader = new UpdateScriptDownloader(tempDir.toString());
  }

  @Test
  void getUpdateScriptPath() {
    assertEquals(tempDir + "/armadillo-reboot.sh", downloader.getUpdateScriptPath());
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.13.0", "5.14.999"})
  void getUpdateScriptUrl_usesCommitHashForOldVersions(String version) {
    String url = downloader.getUpdateScriptUrl(version);
    assertEquals(
        "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/"
            + "70429b0a4ebdb579fdfd66df9bacae3a67866135/scripts/install/armadillo-reboot.sh",
        url);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v5.15.0", "5.15.1", "6.0.0"})
  void getUpdateScriptUrl_usesTagForNewerVersions(String version) {
    String url = downloader.getUpdateScriptUrl(version);
    String expectedVersion = version.replace("v", "");
    assertEquals(
        "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/"
            + "refs/tags/v"
            + expectedVersion
            + "/scripts/install/armadillo-reboot.sh",
        url);
  }

  @Test
  void getUpdateScriptUrl_dev_usesCommitHash() {
    String url = downloader.getUpdateScriptUrl("dev");
    assertEquals(
        "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/"
            + "70429b0a4ebdb579fdfd66df9bacae3a67866135/scripts/install/armadillo-reboot.sh",
        url);
  }

  @Test
  void downloadUpdateScript_downloadsAndMarksExecutable() throws Exception {
    AtomicBoolean called = new AtomicBoolean(false);
    Files.createFile(tempDir.resolve("armadillo-reboot.sh"));

    try (MockedStatic<FileDownloader> mockedDownloader = Mockito.mockStatic(FileDownloader.class)) {
      mockedDownloader
          .when(
              () ->
                  FileDownloader.downloadFile(
                      eq(
                          "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/"
                              + "refs/tags/v5.15.1/scripts/install/armadillo-reboot.sh"),
                      anyString()))
          .thenAnswer(
              interceptor -> {
                called.set(true);
                return null;
              });

      downloader.downloadUpdateScript("v5.15.1");

      assertTrue(called.get());
      assertTrue(tempDir.resolve("armadillo-reboot.sh").toFile().canExecute());
    }
  }

  @Test
  void downloadUpdateScript_throwsWhenScriptFileMissingAfterDownload() {
    try (MockedStatic<FileDownloader> mockedDownloader = Mockito.mockStatic(FileDownloader.class)) {
      mockedDownloader
          .when(() -> FileDownloader.downloadFile(anyString(), anyString()))
          .thenAnswer(interceptor -> null); // does not actually create the file

      assertThrows(ResponseStatusException.class, () -> downloader.downloadUpdateScript("v1.1.0"));
    }
  }
}
