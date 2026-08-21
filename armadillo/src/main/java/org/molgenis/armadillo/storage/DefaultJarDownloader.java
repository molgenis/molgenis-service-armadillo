package org.molgenis.armadillo.storage;

import static java.lang.String.format;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.armadillo.service.GithubApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DefaultJarDownloader implements JarDownloader {

  private static final String RELEASE_DOWNLOAD_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v%s/%s";
  private static final String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";
  private static final String PROGRESS = "progress";
  private static final String DONE = "done";
  private static final String DOWNLOAD_COMPLETE = "Download complete";

  @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}")
  private String armadilloHome;

  private final String jarHome;
  private final GithubApi githubApi;

  public DefaultJarDownloader(@Qualifier("jarHome") String jarHome, GithubApi githubApi) {
    this.jarHome = jarHome;
    this.githubApi = githubApi;
  }

  @Override
  public void downloadFile(String url, String outputFile) throws InterruptedException {
    FileDownloader.downloadFile(url, outputFile);
  }

  @Override
  public void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException {
    FileDownloader.downloadFile(url, outputFile, progressCallback);
  }

  @Override
  public SseEmitter downloadArmadilloJar(String version) {
    SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
    Thread.ofVirtual().start(() -> performDownload(version, emitter));
    return emitter;
  }

  // Package-private and synchronous so tests can exercise it directly on the test
  // thread, where Mockito's static mocking for FileDownloader is actually active.
  void performDownload(String version, SseEmitter emitter) {
    String jarToUpdateTo = getJarFromVersion(version);
    String downloadUrl = String.format(RELEASE_DOWNLOAD_URL, version, jarToUpdateTo);
    String armadilloInstallation = jarHome + File.separator + jarToUpdateTo;
    try {
      if (fileExistsInDir(jarToUpdateTo, jarHome)) {
        emitter.send(SseEmitter.event().name(PROGRESS).data("100")); // already there
      } else {
        downloadFile(
            downloadUrl,
            armadilloInstallation,
            downloadProgress -> updateDownloadProgress(emitter, String.valueOf(downloadProgress)));
      }
      emitter.send(SseEmitter.event().name(DONE).data(DOWNLOAD_COMPLETE));
      emitter.complete();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      emitter.completeWithError(e);
    } catch (Exception e) {
      emitter.completeWithError(e);
    }
  }

  @Override
  public Boolean isValidJar(String version) throws IOException, InterruptedException {
    Boolean isValid = Boolean.FALSE;
    String jarName = getJarFromVersion(version);
    // NB: mirrors pre-refactor behaviour of checking armadilloHome rather than jarHome
    String jarSha = getJarSha(armadilloHome + "/" + jarName);
    String tag = version.startsWith("v") ? version : "v" + version;
    JsonElement githubRelease = githubApi.getReleaseTag(tag);
    String githubSha =
        String.valueOf(
                githubRelease
                    .getAsJsonObject()
                    .get("assets")
                    .getAsJsonArray()
                    .get(0)
                    .getAsJsonObject()
                    .get("digest"))
            .replace("sha256:", "")
            .replaceAll("\"", "");
    if (Objects.equals(githubSha, jarSha)) {
      isValid = Boolean.TRUE;
    }
    return isValid;
  }

  private String getJarSha(String jarPath) throws IOException {
    ByteSource byteSource = com.google.common.io.Files.asByteSource(new File(jarPath));
    HashCode hc = byteSource.hash(Hashing.sha256());
    return hc.toString();
  }

  private String getJarFromVersion(String version) {
    return format(ARMADILLO_JAR, version.replace("v", ""));
  }

  private boolean fileExistsInDir(String filename, String directory) {
    return listFilesForDir(directory).contains(filename);
  }

  private Set<String> listFilesForDir(String dir) {
    return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  private void updateDownloadProgress(SseEmitter emitter, String progress) {
    try {
      emitter.send(SseEmitter.event().name(PROGRESS).data(progress));
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
  }
}
