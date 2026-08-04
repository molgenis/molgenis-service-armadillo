package org.molgenis.armadillo.service;

import static java.lang.String.format;
import static org.molgenis.armadillo.storage.FileDownloader.downloadFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.config.ConfigFile;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

  // Constants
  private static final String REBOOT_SCRIPT = "armadillo-reboot.sh";
  private static final String RELEASE_URL =
      "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/latest";
  private static final String REBOOT_SCRIPT_URL =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/%s/scripts/install/%s";
  private static final String RELEASE_DOWNLOAD_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v%s/%s";
  private static final String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";
  private static final String PROGRESS = "progress";
  private static final String DONE = "done";
  private static final String DOWNLOAD_COMPLETE = "Download complete";

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

  private final RebootScriptRunner scriptRunner;
  private final ConfigFile appConfigFile;
  private final String jarHome;

  private final HttpClient httpClient;

  @Autowired
  public ManagementService(
      @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
          String armadilloConfigFile,
      @Autowired BuildProperties buildProperties,
      @Autowired RebootScriptRunner scriptRunner,
      HttpClient httpClient,
      @Qualifier("jarHome") String jarHome,
      ConfigFile configFile) {
    this.httpClient = httpClient;
    this.buildProperties = buildProperties;
    this.scriptRunner = scriptRunner;
    this.armadilloConfigFile = armadilloConfigFile;
    this.jarHome = jarHome;
    this.appConfigFile = configFile;
  }

  // This will programmatically restart the application.
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

  // This will trigger a script that kills armadillo, after which it will startup again.
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
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(RELEASE_URL)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      return JsonParser.parseString(response.body()).getAsJsonObject();
    } else {
      throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()));
    }
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
    // TODO: simplify this after update script is available on version, then it should simply return
    // refs/tags/vx.y.z
    // if script not available yet on current release:
    String scriptVersionTag = "11f96b1c227d04ccb8870fafe08dbf3206ca172c";
    if (!version.equals("dev")) {
      version = version.replace("v", "");
    }
    String[] versionSplit = version.split("\\.");
    try {
      if (Integer.parseInt(versionSplit[0]) > 5
          || (Integer.parseInt(versionSplit[0]) == 5 && Integer.parseInt(versionSplit[1]) >= 15)) {
        scriptVersionTag = "refs/tags/v" + version;
      }
    } catch (NumberFormatException ignored) {
      // when dev
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
    return jarHome + File.separator + getJarFromVersion(version);
  }

  String getJarFromVersion(String version) {
    return String.format(ARMADILLO_JAR, version.replace("v", ""));
  }

  boolean isValidVersion(String version) {
    return version.matches("v?\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?") || version.equals("dev");
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
          "-i",
          getJavaProcessId(getProcessName()),
          "-u");
    } else
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
  }

  String getUpdateScriptPath() {
    return format("%s/%s", jarHome, REBOOT_SCRIPT);
  }

  String getUpdateScriptUrl(String armadilloVersion) {
    String scriptVersionTag = getScriptVersionTag(armadilloVersion);
    return String.format(REBOOT_SCRIPT_URL, scriptVersionTag, REBOOT_SCRIPT);
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

  public Set<String> listAvailableJars() {
    return listFilesForDir(jarHome).stream()
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
      emitter.send(SseEmitter.event().name(PROGRESS).data(progress));
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
  }

  public SseEmitter downloadArmadilloJar(String version) {
    if (isValidVersion(version)) {
      SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
      String jarToUpdateTo = getJarFromVersion(version);
      String downloadUrl = String.format(RELEASE_DOWNLOAD_URL, version, jarToUpdateTo);
      String armadilloInstallation = jarHome + File.separator + jarToUpdateTo;
      // Run download in background thread — SSE must not block the request thread
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  if (fileExistsInDir(jarToUpdateTo, jarHome)) {
                    emitter.send(SseEmitter.event().name(PROGRESS).data("100")); // already there
                  } else {
                    downloadFile(
                        downloadUrl,
                        armadilloInstallation,
                        downloadProgress ->
                            updateDownloadProgress(emitter, String.valueOf(downloadProgress)));
                  }
                  emitter.send(SseEmitter.event().name(DONE).data(DOWNLOAD_COMPLETE));
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
    appConfigFile.update(oidcDetails);
    hardRestartApplication();
  }
}
