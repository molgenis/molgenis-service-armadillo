package org.molgenis.armadillo.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.service.GithubApi;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class DefaultJarDownloaderTest {

  private static final String VERSION = "1.0.0";
  private static final String JAR_NAME = "molgenis-armadillo-1.0.0.jar";
  private static final String EXPECTED_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v1.0.0/" + JAR_NAME;

  @Mock private GithubApi githubApi;

  @TempDir private Path tempDir;

  private DefaultJarDownloader jarDownloader;
  private String jarHome;

  @BeforeEach
  void setUp() {
    jarHome = tempDir.toString();
    jarDownloader = new DefaultJarDownloader(jarHome, githubApi);
    // @Value field isn't populated outside of a Spring context
    ReflectionTestUtils.setField(jarDownloader, "armadilloHome", jarHome);
  }

  @AfterEach
  void clearInterruptFlag() {
    // performDownload() calls Thread.currentThread().interrupt() on the calling
    // thread in the InterruptedException branch; reset it so it doesn't leak
    // into other tests.
    Thread.interrupted();
  }

  @Test
  void performDownload_downloadsJarWhenNotPresentAndCompletes() throws Exception {
    SseEmitter emitter = mock(SseEmitter.class);
    String expectedInstallPath = jarHome + File.separator + JAR_NAME;

    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      fileDownloader
          .when(
              () ->
                  FileDownloader.downloadFile(
                      eq(EXPECTED_URL), eq(expectedInstallPath), any(LongConsumer.class)))
          .thenAnswer(
              invocation -> {
                LongConsumer callback = invocation.getArgument(2);
                callback.accept(50L);
                return null;
              });

      jarDownloader.performDownload(VERSION, emitter);

      fileDownloader.verify(
          () ->
              FileDownloader.downloadFile(
                  eq(EXPECTED_URL), eq(expectedInstallPath), any(LongConsumer.class)));
    }

    // one progress event from the callback (50) + one "done" event
    verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter).complete();
    verify(emitter, never()).completeWithError(any());
  }

  @Test
  void performDownload_skipsDownloadWhenJarAlreadyExists() throws IOException {
    // put a file with the expected jar name in jarHome so fileExistsInDir() is true
    Files.createFile(tempDir.resolve(JAR_NAME));

    SseEmitter emitter = mock(SseEmitter.class);

    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      jarDownloader.performDownload(VERSION, emitter);

      fileDownloader.verifyNoInteractions();
    }

    // one "already there" 100% progress event + one "done" event
    verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter).complete();
    verify(emitter, never()).completeWithError(any());
  }

  @Test
  void performDownload_completesWithErrorOnInterruptedException() {
    SseEmitter emitter = mock(SseEmitter.class);

    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      fileDownloader
          .when(() -> FileDownloader.downloadFile(any(), any(), any(LongConsumer.class)))
          .thenThrow(new InterruptedException("boom"));

      jarDownloader.performDownload(VERSION, emitter);
    }

    ArgumentCaptor<InterruptedException> captor =
        ArgumentCaptor.forClass(InterruptedException.class);
    verify(emitter).completeWithError(captor.capture());
    assertThat(captor.getValue()).hasMessage("boom");
    verify(emitter, never()).complete();
    assertThat(Thread.interrupted()).isTrue(); // and clears the flag for this test's thread
  }

  @Test
  void performDownload_completesWithErrorOnUnexpectedException() {
    SseEmitter emitter = mock(SseEmitter.class);
    RuntimeException failure = new RuntimeException("network exploded");

    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      fileDownloader
          .when(() -> FileDownloader.downloadFile(any(), any(), any(LongConsumer.class)))
          .thenThrow(failure);

      jarDownloader.performDownload(VERSION, emitter);
    }

    verify(emitter).completeWithError(failure);
    verify(emitter, never()).complete();
  }

  @Test
  void isValidJar_returnsTrue_whenShaMatchesGithub() throws Exception {
    byte[] content = "hello armadillo".getBytes(StandardCharsets.UTF_8);
    Files.write(tempDir.resolve(JAR_NAME), content);
    String sha256 = sha256Hex(content);

    when(githubApi.getFromJarAsset(VERSION, "digest")).thenReturn("sha256:" + sha256);

    assertThat(jarDownloader.isValidJar(VERSION)).isTrue();
  }

  @Test
  void isValidJar_returnsFalse_whenShaDoesNotMatchGithub() throws Exception {
    byte[] content = "hello armadillo".getBytes(StandardCharsets.UTF_8);
    Files.write(tempDir.resolve(JAR_NAME), content);

    when(githubApi.getFromJarAsset(VERSION, "digest")).thenReturn("sha256:deadbeef");

    assertThat(jarDownloader.isValidJar(VERSION)).isFalse();
  }

  @Test
  void doesJarFit_delegatesToDiskSpaceChecker() throws Exception {
    when(githubApi.getFromJarAsset(VERSION, "size")).thenReturn("123456");

    try (MockedStatic<DiskSpaceChecker> diskSpaceChecker = mockStatic(DiskSpaceChecker.class)) {
      diskSpaceChecker.when(() -> DiskSpaceChecker.fitsOnDisk(123456L)).thenReturn(true);

      assertThat(jarDownloader.doesJarFit(VERSION)).isTrue();

      diskSpaceChecker.verify(() -> DiskSpaceChecker.fitsOnDisk(123456L));
    }
  }

  @Test
  void doesJarFit_returnsFalse_whenItDoesNotFit() throws Exception {
    when(githubApi.getFromJarAsset(VERSION, "size")).thenReturn("999999999");

    try (MockedStatic<DiskSpaceChecker> diskSpaceChecker = mockStatic(DiskSpaceChecker.class)) {
      diskSpaceChecker.when(() -> DiskSpaceChecker.fitsOnDisk(999999999L)).thenReturn(false);

      assertThat(jarDownloader.doesJarFit(VERSION)).isFalse();
    }
  }

  @Test
  void downloadFile_delegatesToFileDownloaderWithoutProgressCallback() throws Exception {
    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      jarDownloader.downloadFile("http://example.org/x.jar", "/tmp/x.jar");

      fileDownloader.verify(
          () -> FileDownloader.downloadFile("http://example.org/x.jar", "/tmp/x.jar"));
    }
  }

  @Test
  void downloadFile_delegatesToFileDownloaderWithProgressCallback() throws Exception {
    LongConsumer progressCallback = value -> {};

    try (MockedStatic<FileDownloader> fileDownloader = mockStatic(FileDownloader.class)) {
      jarDownloader.downloadFile("http://example.org/x.jar", "/tmp/x.jar", progressCallback);

      fileDownloader.verify(
          () ->
              FileDownloader.downloadFile(
                  "http://example.org/x.jar", "/tmp/x.jar", progressCallback));
    }
  }

  private static String sha256Hex(byte[] content) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(content);
    StringBuilder hex = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}
