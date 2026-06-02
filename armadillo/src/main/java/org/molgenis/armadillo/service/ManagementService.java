package org.molgenis.armadillo.service;

import static java.lang.String.format;
import static org.molgenis.armadillo.controller.FileDownloader.downloadFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.controller.RebootScriptRunner;
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

  @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
  String armadilloConfigFile;

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

  @Autowired BuildProperties buildProperties;

  // location of update log
  private String updateLogPath;

  private final RebootScriptRunner scriptRunner;

  // Constants
  String REBOOT_SCRIPT = "armadillo-reboot.sh";
  String RELEASE_URL =
      "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/latest";
  String REBOOT_SCRIPT_URL =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/%s/scripts/install/%s";
  String RELEASE_DOWNLOAD_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v%s/%s";
  String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";
  String TAG = "tag_name";
  String BACKUP_EXT = ".bak";
  String PROGRESS = "progress";
  String DONE = "done";
  String DOWNLOAD_COMPLETE = "Download complete";
  String DEV = "DEV";

  private final HttpClient httpClient;

  @Autowired
  public ManagementService(
      @Value("${stdout.log.path:./logs/armadillo.log}") String logPath,
      @Value("${update.log.path:#{null}}") String updatePath,
      HttpClient httpClient) {
    this.httpClient = httpClient;
    if (updatePath == null) {
      var splittedLogFilepath = logPath.split(File.separator);
      // if updateLog not set, take path of stdout log and put update.log in same dir
      updateLogPath =
          String.join(
                  File.separator,
                  Arrays.copyOf(splittedLogFilepath, splittedLogFilepath.length - 1))
              + File.separator
              + "update.log";
    }
    scriptRunner = new RebootScriptRunner(updateLogPath, getJarHome());
  }

  public void softRestartApplication() {
    ArmadilloServiceApplication.restart();
  }

  public void hardRestartApplication() throws IOException {
    scriptRunner.runRebootScript(
        getUpdateScriptPath(), "-p", armadilloHome, "-v", "", "-m", armadilloMode);
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

  public String getReleaseVersion(JsonElement release) {
    return ((JsonObject) release).get(TAG).getAsString();
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
    if (Objects.equals(versionSplit[0], "5") && Integer.parseInt(versionSplit[1]) < 14) {
      scriptVersionTag = "6f815bb32e5677ce17680d262344d2f4e3c6106e";
    } else {
      scriptVersionTag = "refs/tags/v" + version;
    }
    return scriptVersionTag;
  }

  private boolean fileExistsInDir(String filename, String directory) {
    Set<String> foundFiles = listFilesForDir(directory);
    return foundFiles.contains(filename);
  }

  private void updateApplicationConfig(OidcDetails oidcDetails) {
    try (BufferedReader br = new BufferedReader(new FileReader(armadilloConfigFile))) {
      List<String> lines = br.lines().collect(Collectors.toList());
      String existingConfig = String.join(System.lineSeparator(), lines) + System.lineSeparator();
      String newConfig = transformConfig(lines, oidcDetails);

      FileUtils.writeStringToFile(
          new File(armadilloConfigFile + BACKUP_EXT),
          existingConfig,
          Charset.defaultCharset(),
          false);
      FileUtils.writeStringToFile(
          new File(armadilloConfigFile), newConfig, Charset.defaultCharset(), false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String transformConfig(List<String> lines, OidcDetails oidcDetails) {
    ConfigParseState state = new ConfigParseState();
    StringBuilder newConfig = new StringBuilder();

    for (String line : lines) {
      newConfig.append(transformLine(line, state, oidcDetails));
      newConfig.append(System.lineSeparator());
    }
    return newConfig.toString();
  }

  private String transformLine(String line, ConfigParseState state, OidcDetails oidcDetails) {
    if (line.strip().startsWith("#")) return line;

    if (line.contains("provider:")) {
      state.providerFound = true;
      return line;
    }
    if (line.contains("registration:")) {
      state.registrationFound = true;
      return line;
    }
    if (line.contains("resourceserver:")) {
      state.resourceServerFound = true;
      return line;
    }
    if (line.contains("jwt:") && state.resourceServerFound) {
      state.resourceServerJwtFound = true;
      return line;
    }
    if (line.contains("opaquetoken") && state.resourceServerFound) {
      state.resourceServerOpaqueFound = true;
      return line;
    }

    if (line.contains("molgenis:")) {
      if (state.providerFound) state.providerMolgenisFound = true;
      if (state.registrationFound) state.registrationMolgenisFound = true;
      return line;
    }

    if (line.contains("issuer-uri:")) {
      if (state.providerMolgenisFound && !state.issuerUriUpdated) {
        state.issuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getIssuerUri());
      }
      if (state.resourceServerJwtFound && !state.deviceIssuerUriUpdated) {
        state.deviceIssuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceIssuerUri());
      }
    }

    if (line.contains("client-id:")) {
      if (state.registrationMolgenisFound && !state.clientIdUpdated) {
        state.clientIdUpdated = true;
        return replaceValue(line, oidcDetails.getClientId());
      }
      if (state.resourceServerOpaqueFound && !state.deviceClientIdUpdated) {
        state.deviceClientIdUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceClientId());
      }
    }

    if (line.contains("client-secret:")
        && state.registrationMolgenisFound
        && !state.clientSecretUpdated) {
      state.clientSecretUpdated = true;
      return replaceValue(line, oidcDetails.getClientSecret());
    }

    return line;
  }

  private String replaceValue(String line, String newValue) {
    return line.split(":")[0] + ": " + newValue;
  }

  public void deleteJar(String version) {
    String appVersion = buildProperties.getVersion();
    String fileToDelete = getJarPathFromVersion(version);
    if (appVersion.equals(version)) {
      throw new StorageException("Cannot delete file: jar is currently running.");
    } else {
      File armadilloJar = new File(fileToDelete);
      if (!armadilloJar.delete()) {
        throw new StorageException("Cannot delete file: " + armadilloJar.getName());
      }
    }
  }

  private String getJarPathFromVersion(String version) {
    return getJarHome() + File.separator + getJarFromVersion(version);
  }

  String getJarFromVersion(String version) {
    return String.format(ARMADILLO_JAR, version.replace("v", ""));
  }

  boolean isValidVersion(String version) {
    return version.matches("v?\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
  }

  public void triggerUpdate(String version) throws IOException {
    if (isValidVersion(version)) {
      scriptRunner.runRebootScript(
          getUpdateScriptPath(), "-p", armadilloHome, "-v", version, "-m", armadilloMode, "-u");
    } else
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
  }

  // Simple mutable state carrier — private inner class or a record (Java 16+)
  private static class ConfigParseState {
    boolean providerFound, providerMolgenisFound;
    boolean registrationFound, registrationMolgenisFound;
    boolean resourceServerFound, resourceServerJwtFound, resourceServerOpaqueFound;
    boolean clientIdUpdated, clientSecretUpdated;
    boolean issuerUriUpdated, deviceIssuerUriUpdated, deviceClientIdUpdated;
  }

  String getUpdateScriptPath() {
    return format("%s/%s", getJarHome(), REBOOT_SCRIPT);
  }

  String getUpdateScriptUrl(String armadilloVersion) {
    armadilloVersion = armadilloVersion.replace("v", "");
    String scriptVersionTag = getScriptVersionTag(armadilloVersion);
    return String.format(REBOOT_SCRIPT_URL, scriptVersionTag, REBOOT_SCRIPT);
  }

  public void downloadUpdateScript(String armadilloVersion) throws InterruptedException {
    if (isValidVersion(armadilloVersion)) {
      String updateScriptPath = getUpdateScriptPath();
      downloadFile(getUpdateScriptUrl(armadilloVersion), updateScriptPath);
      // give permissions to run the script
      File script = new File(updateScriptPath);
      script.setExecutable(true, false);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  String getJarHome() {
    if (Objects.equals(armadilloMode, DEV)) {
      return format("%s/build/libs", armadilloHome);
    } else {
      return format("%s", armadilloHome);
    }
  }

  public Set<String> listAvailableJars() {
    return listFilesForDir(getJarHome()).stream()
        .filter((name) -> name.endsWith(".jar"))
        .collect(Collectors.toSet());
  }

  private Set<String> listFilesForDir(String dir) {
    return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  public SseEmitter downloadArmadilloJar(String version) throws IOException, InterruptedException {
    if (isValidVersion(version)) {
      SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
      String armadilloJar = getJarFromVersion(version);
      String downloadUrl = String.format(RELEASE_DOWNLOAD_URL, version, armadilloJar);
      String armadilloInstallation = getJarHome() + File.separator + armadilloJar;
      // Run download in background thread — SSE must not block the request thread
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  if (fileExistsInDir(armadilloJar, getJarHome())) {
                    emitter.send(SseEmitter.event().name(PROGRESS).data("100")); // already there
                  } else {
                    downloadFile(
                        downloadUrl,
                        armadilloInstallation,
                        progress -> {
                          try {
                            emitter.send(
                                SseEmitter.event().name(PROGRESS).data(String.valueOf(progress)));
                          } catch (IOException e) {
                            emitter.completeWithError(e);
                          }
                        });
                  }
                  emitter.send(SseEmitter.event().name(DONE).data(DOWNLOAD_COMPLETE));
                  emitter.complete();
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
    updateApplicationConfig(oidcDetails);
    hardRestartApplication();
  }
}
