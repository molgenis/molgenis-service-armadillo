package org.molgenis.armadillo.service;

import static java.lang.String.format;
import static org.molgenis.armadillo.storage.FileDownloader.downloadFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.config.ApplicationConfigUpdater;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {
  @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}")
  String armadilloHome;

  @Value("${armadillo.armadillo-mode:PROD}")
  String armadilloMode;

  @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
  String clientId;

  @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
  String clientSecret;

  @Value("${spring.security.oauth2.client.provider.molgenis.issuer-uri:#{null}}")
  String issuerUri;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
  String deviceIssuerUri;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id:#{null}}")
  String deviceClientId;

  @Value("${armadillo.docker-run-in-container:false}")
  private boolean runningInContainer;

  BuildProperties buildProperties;
  String armadilloConfigFile;

  // location of update log
  private String updateLogPath;

  private final RebootScriptRunner scriptRunner;

  private final ApplicationConfigUpdater appConfigUpdater;

  // Constants
  String rebootScript = "armadillo-reboot.sh";
  String releaseUrl =
      "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/latest";
  String rebootScriptUrl =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/%s/scripts/install/%s";
  String releaseDownloadUrl =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v%s/%s";
  String armadilloJar = "molgenis-armadillo-%s.jar";
  String tag = "tag_name";
  String backupExt = ".bak";
  String progress = "progress";
  String done = "done";
  String downloadComplete = "Download complete";
  String dev = "DEV";

  private final HttpClient httpClient;

  @Autowired
  public ManagementService(
      @Value("${stdout.log.path:./logs/armadillo.log}") String logPath,
      @Value("${update.log.path:#{null}}") String updatePath,
      @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
          String armadilloConfigFile,
      @Autowired BuildProperties buildProperties,
      HttpClient httpClient) {
    this.httpClient = httpClient;
    this.buildProperties = buildProperties;
    if (updatePath == null) {
      var splittedLogFilepath = logPath.split(Pattern.quote(File.separator));
      // if updateLog not set, take path of stdout log and put update.log in same dir
      updateLogPath =
          String.join(
                  File.separator,
                  Arrays.copyOf(splittedLogFilepath, splittedLogFilepath.length - 1))
              + File.separator
              + "update.log";
    }
    scriptRunner = new RebootScriptRunner(updateLogPath, getJarHome());
    this.armadilloConfigFile = armadilloConfigFile;
    appConfigUpdater = new ApplicationConfigUpdater(armadilloConfigFile);
  }

  public void softRestartApplication() {
    ArmadilloServiceApplication.restart();
  }

  private String getProcessName() {
    return ManagementFactory.getRuntimeMXBean().getName();
  }

  String getJavaProcessId(String processName) {
    return Arrays.stream(processName.split("@")).toList().getFirst();
  }

  void throwWhenRunningInContainer(String method) throws UnsupportedOperationException {
    if (runningInContainer) {
      throw new UnsupportedOperationException(
          "Cannot execute " + method + "because armadillo is running within a docker container");
    }
  }

  public void hardRestartApplication() throws IOException {
    throwWhenRunningInContainer("hard restart");
    scriptRunner.runRebootScript(
        getUpdateScriptPath(),
        "-p",
        armadilloHome,
        "-v",
        "",
        "-m",
        armadilloMode,
        "-i",
        getJavaProcessId(getProcessName()),
        "-c",
        armadilloConfigFile.replace("/application.yml", ""));
  }

  public JsonElement getLastRelease() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(releaseUrl)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      return JsonParser.parseString(response.body()).getAsJsonObject();
    } else {
      throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()));
    }
  }

  public String getReleaseVersion(JsonElement release) {
    return ((JsonObject) release).get(tag).getAsString();
  }

  public Map<String, String> getCurrentOidcConfig() {
    Map<String, String> currentConfig = new HashMap<>();
    currentConfig.put("issuerUri", issuerUri);
    currentConfig.put("clientId", clientId);
    currentConfig.put("clientSecret", clientSecret);
    currentConfig.put("deviceClientId", deviceClientId);
    currentConfig.put("deviceIssuerUri", deviceIssuerUri);
    return currentConfig;
  }

  private String getScriptVersionTag(String version) {
    String[] versionSplit = version.split("\\.");
    String scriptVersionTag;
    // if script not available yet on current release:
    if (Objects.equals(versionSplit[0], "5") && Integer.parseInt(versionSplit[1]) < 15) {
      scriptVersionTag = "2b0c42c171c62074ec3fd119187cb24a672040a1";
    } else {
      scriptVersionTag = "refs/tags/v" + version;
    }
    return scriptVersionTag;
  }

  private boolean fileExistsInDir(String filename, String directory) {
    Set<String> foundFiles = listFilesForDir(directory);
    return foundFiles.contains(filename);
  }

  public void deleteJar(String version) {
    String appVersion = buildProperties.getVersion();
    String fileToDelete = getJarPathFromVersion(version);
    if (appVersion.equals(version)) {
      throw new StorageException("Cannot delete file: jar is currently running.");
    } else {
      Path path = Paths.get(fileToDelete);
      try {
        Files.delete(path);
      } catch (NoSuchFileException x) {
        throw new StorageException(format("%s: no such file or directory%n", path));
      } catch (IOException x) {
        throw new StorageException(
            format("Cannot delete file: [%s] because %s.", path.getFileName(), x));
      }
    }
  }

  private String getJarPathFromVersion(String version) {
    return getJarHome() + File.separator + getJarFromVersion(version);
  }

  String getJarFromVersion(String version) {
    return String.format(armadilloJar, version.replace("v", ""));
  }

  boolean isValidVersion(String version) {
    return version.matches("v?\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
  }

  public void triggerUpdate(String version) throws IOException {
    throwWhenRunningInContainer("hard restart");
    if (isValidVersion(version)) {
      scriptRunner.runRebootScript(
          getUpdateScriptPath(),
          "-p",
          armadilloHome,
          "-v",
          version,
          "-m",
          armadilloMode,
          "-u",
          "-i",
          getJavaProcessId(getProcessName()));
    } else
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
  }

  String getUpdateScriptPath() {
    return format("%s/%s", getJarHome(), rebootScript);
  }

  String getUpdateScriptUrl(String armadilloVersion) {
    armadilloVersion = armadilloVersion.replace("v", "");
    String scriptVersionTag = getScriptVersionTag(armadilloVersion);
    return String.format(rebootScriptUrl, scriptVersionTag, rebootScript);
  }

  public void downloadUpdateScript(String armadilloVersion) throws InterruptedException {
    if (isValidVersion(armadilloVersion)) {
      String updateScriptPath = getUpdateScriptPath();
      downloadFile(getUpdateScriptUrl(armadilloVersion), updateScriptPath);
      // give permissions to run the script
      File script = new File(updateScriptPath);
      boolean isExecutableSet = script.setExecutable(true, false);
      if (!isExecutableSet) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to set file as executable: " + updateScriptPath);
      }
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  String getJarHome() {
    if (Objects.equals(armadilloMode, dev)) {
      return format("%s/build/libs", armadilloHome);
    } else {
      return format("%s", armadilloHome);
    }
  }

  public Set<String> listAvailableJars() {
    return listFilesForDir(getJarHome()).stream()
        .filter(name -> name.endsWith(".jar"))
        .collect(Collectors.toSet());
  }

  private Set<String> listFilesForDir(String dir) {
    return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  private void updateDownloadProgress(SseEmitter emitter, String progress) {
    try {
      emitter.send(SseEmitter.event().name(this.progress).data(progress));
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
  }

  public SseEmitter downloadArmadilloJar(String version) {
    if (isValidVersion(version)) {
      SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
      String jarToUpdateTo = getJarFromVersion(version);
      String downloadUrl = String.format(releaseDownloadUrl, version, jarToUpdateTo);
      String armadilloInstallation = getJarHome() + File.separator + jarToUpdateTo;
      // Run download in background thread — SSE must not block the request thread
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  if (fileExistsInDir(jarToUpdateTo, getJarHome())) {
                    emitter.send(SseEmitter.event().name(progress).data("100")); // already there
                  } else {
                    downloadFile(
                        downloadUrl,
                        armadilloInstallation,
                        downloadProgress ->
                            updateDownloadProgress(emitter, String.valueOf(downloadProgress)));
                  }
                  emitter.send(SseEmitter.event().name(done).data(downloadComplete));
                  emitter.complete();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  emitter.completeWithError(e);
                } catch (Exception e) {
                  emitter.completeWithError(e);
                }
              });
      return emitter; // 5 min timeout
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  public void saveNewOidcConfig(OidcDetails oidcDetails) throws IOException {
    throwWhenRunningInContainer("update oidc config");
    appConfigUpdater.updateApplicationConfig(oidcDetails);
    hardRestartApplication();
  }
}
