package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.TestHelpers.setField;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.service.GithubApi;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class DefaultJarDownloaderTest {

  @Mock GithubApi githubApi;

  @TempDir Path tempDir;

  DefaultJarDownloader downloader;

  @BeforeEach
  void setUp() throws Exception {
    downloader = new DefaultJarDownloader(tempDir.toString(), githubApi);
    setField(downloader, "armadilloHome", tempDir.toString());
  }

  @Test
  void isValidJar_returnsTrueWhenShaMatchesGithubDigest() throws Exception {
    String jarName = "molgenis-armadillo-5.14.0.jar";
    Path jarFile = tempDir.resolve(jarName);
    Files.writeString(jarFile, "jar-content");
    String expectedSha =
        com.google.common.io.Files.asByteSource(jarFile.toFile()).hash(Hashing.sha256()).toString();

    when(githubApi.getReleaseTag("v5.14.0")).thenReturn(releaseWithDigest(expectedSha));

    assertTrue(downloader.isValidJar("5.14.0"));
  }

  @Test
  void isValidJar_returnsFalseWhenShaDiffersFromGithubDigest() throws Exception {
    String jarName = "molgenis-armadillo-5.14.0.jar";
    Path jarFile = tempDir.resolve(jarName);
    Files.writeString(jarFile, "jar-content");

    when(githubApi.getReleaseTag("v5.14.0")).thenReturn(releaseWithDigest("some-other-sha"));

    assertFalse(downloader.isValidJar("5.14.0"));
  }

  @Test
  void isValidJar_addsVPrefixWhenLookingUpTag() throws Exception {
    String jarName = "molgenis-armadillo-5.14.0.jar";
    Path jarFile = tempDir.resolve(jarName);
    Files.writeString(jarFile, "jar-content");
    String expectedSha =
        com.google.common.io.Files.asByteSource(jarFile.toFile()).hash(Hashing.sha256()).toString();

    when(githubApi.getReleaseTag("v5.14.0")).thenReturn(releaseWithDigest(expectedSha));

    downloader.isValidJar("5.14.0"); // no leading "v" in input

    verify(githubApi).getReleaseTag("v5.14.0");
  }

  private JsonElement releaseWithDigest(String digest) {
    JsonObject asset = new JsonObject();
    asset.addProperty("digest", digest);
    JsonArray assets = new JsonArray();
    assets.add(asset);
    JsonObject release = new JsonObject();
    release.add("assets", assets);
    return release;
  }

  @Test
  void downloadArmadilloJar_downloadsAndWritesFile() throws Exception {
    SseEmitter emitter = new SseEmitter();

    try (MockedStatic<FileDownloader> mockedDownloader = Mockito.mockStatic(FileDownloader.class)) {
      mockedDownloader
          .when(() -> FileDownloader.downloadFile(anyString(), anyString(), any()))
          .thenAnswer(
              interceptor -> {
                String outputFile = interceptor.getArgument(1);
                LongConsumer progressCallback = interceptor.getArgument(2);
                Files.writeString(Path.of(outputFile), "jar-bytes");
                progressCallback.accept(100L);
                return null;
              });

      downloader.performDownload("5.12.2", emitter);

      Path jarPath = tempDir.resolve("molgenis-armadillo-5.12.2.jar");
      assertTrue(Files.exists(jarPath));
      assertEquals("jar-bytes", Files.readString(jarPath));
      mockedDownloader.verify(() -> FileDownloader.downloadFile(anyString(), anyString(), any()));
    }
  }

  @Test
  void downloadArmadilloJar_skipsDownloadWhenJarAlreadyPresent() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.12.2.jar"));
    SseEmitter emitter = new SseEmitter();

    try (MockedStatic<FileDownloader> mockedDownloader = Mockito.mockStatic(FileDownloader.class)) {
      downloader.performDownload("5.12.2", emitter);

      mockedDownloader.verify(
          () -> FileDownloader.downloadFile(anyString(), anyString(), any()), Mockito.never());
    }
  }
}
