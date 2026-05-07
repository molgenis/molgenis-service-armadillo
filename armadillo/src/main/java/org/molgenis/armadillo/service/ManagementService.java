package org.molgenis.armadillo.service;

import static java.lang.String.format;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LoggingException;
import org.molgenis.armadillo.ArmadilloServiceApplication;
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

  // Constants
  String updateScript = "armadillo-reboot.sh";
  String RELEASE_URL = "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases";
  String UPDATE_SCRIPT_URL =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v%s/scripts/install/%s";
  String RELEASE_DOWNLOAD_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/v%s/%s";
  String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";
  String TAG = "tag_name";
  String BACKUP_EXT = ".bak";
  String PROGRESS = "progress";
  String DONE = "done";
  String CREATION_TIME = "creationTime";
  String DOWNLOAD_COMPLETE = "Download complete";
  String NOT_FOUND = "not found";
  String DEV = "DEV";

  public ManagementService(
      @Value("${stdout.log.path:./logs/armadillo.log}") String logPath,
      @Value("${update.log.path:#{null}}") String updatePath) {
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
  }

  public void softRestartApplication() {
    ArmadilloServiceApplication.restart();
  }

  public void hardRestartApplication() {
    runRestartScriptInDifferentThread("", false);
  }

  private JsonArray getReleases() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(RELEASE_URL)).GET().build();
    HttpResponse<String> response =
        HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      return JsonParser.parseString(response.body()).getAsJsonArray();
    } else {
      throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()));
    }
  }

  public JsonElement getLastRelease() throws IOException, InterruptedException {
    JsonArray armadilloReleases = getReleases();
    Optional<JsonElement> lastRelease =
        StreamSupport.stream(armadilloReleases.spliterator(), false)
            .filter(
                release -> ((JsonObject) release).get("prerelease").getAsString().equals("false"))
            .findFirst();
    return lastRelease.get();
  }

  public JsonElement getReleaseByVersion(String version) throws IOException, InterruptedException {
    JsonArray armadilloReleases = getReleases();
    Optional<JsonElement> release =
        StreamSupport.stream(armadilloReleases.spliterator(), false)
            .filter(r -> ((JsonObject) r).get(TAG).getAsString().equals(version))
            .findFirst();
    return release.get();
  }

  private LocalDateTime getLastReleaseDate(JsonElement lastRelease) {
    String rawDate = ((JsonObject) lastRelease).get("published_at").getAsString();
    DateTimeFormatter inputFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    return LocalDateTime.parse(rawDate, inputFormatter);
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

  private File getUpdateLogFile() throws IOException {
    File logFile = new File(updateLogPath);
    logFile.getParentFile().mkdirs();
    if (!logFile.exists()) logFile.createNewFile();
    return logFile;
  }

  private String createPythonScript(
      String command, String logFilePath, String version, Boolean isUpdate) {
    String scriptTemplate =
        """
                          import os, sys, subprocess
                          if os.fork() > 0:
                              sys.exit(0)
                          os.setsid()
                          if os.fork() > 0:
                              sys.exit(0)
                          with open('%s', 'a') as log:
                              subprocess.run(%s, stdout=log, stderr=log, stdin=subprocess.DEVNULL)
                          """;
    String pythonList;
    if (isUpdate) {
      pythonList =
          buildPythonList(command, "-p", armadilloHome, "-v", version, "-m", armadilloMode, "-u");
    } else {
      pythonList =
          buildPythonList(command, "-p", armadilloHome, "-v", version, "-m", armadilloMode);
    }
    return String.format(scriptTemplate, logFilePath, pythonList);
  }

  private void runRestartScriptInDifferentThread(String version, Boolean isUpdate) {
    Thread updateThread =
        new Thread(
            () -> {
              try {
                String command;
                if (Objects.equals(armadilloMode, DEV)) {
                  command = armadilloHome + "/scripts/install/" + updateScript;
                } else {
                  command = armadilloHome + updateScript;
                }
                File logFile = getUpdateLogFile();
                Thread logTailer = startLogTailer(logFile);
                Thread.sleep(200);

                String pythonScript =
                    createPythonScript(command, logFile.getAbsolutePath(), version, isUpdate);
                // Double-fork via Python to fully detach from JVM process group

                ProcessBuilder processBuilder = new ProcessBuilder("python3", "-c", pythonScript);
                processBuilder.redirectInput(new File("/dev/null"));
                processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
                Process python = processBuilder.start();
                python.waitFor();
                logTailer.join(5000);
              } catch (IOException | InterruptedException e) {
                e.printStackTrace();
              }
            });
    updateThread.setDaemon(false);
    updateThread.setName("update-armadillo");
    updateThread.start();
  }

  // Builds a Python list literal e.g. ['/path/script', '-p', '/home']
  private String buildPythonList(String... args) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      sb.append("'").append(args[i].replace("'", "\\'")).append("'");
      if (i < args.length - 1) sb.append(", ");
    }
    sb.append("]");
    return sb.toString();
  }

  private Thread startLogTailer(File logFile) throws IOException {
    Thread tailer =
        new Thread(
            () -> {
              try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                reader.skip(logFile.length());
                while (!Thread.currentThread().isInterrupted()) {
                  String line = reader.readLine();
                  if (line == null) {
                    Thread.sleep(100);
                  }
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (IOException e) {
                throw new LoggingException(
                    format("[UPDATE SCRIPT]: Log tailer error: %s\"", e.getMessage()));
              }
            });
    tailer.setDaemon(true);
    tailer.setName("update-log-tailer");
    tailer.start();
    return tailer;
  }

  private String getScriptVersionTag(String version) {
    String[] versionSplit = version.split("\\.");
    String scriptVersionTag;
    // if script not available yet on current release:
    if (Objects.equals(versionSplit[0], "5") && Integer.parseInt(versionSplit[1]) < 14) {
      scriptVersionTag = "6f815bb32e5677ce17680d262344d2f4e3c6106e";
    } else if (version.startsWith("v")) {
      scriptVersionTag = "refs/tags/" + version;
    } else {
      scriptVersionTag = "refs/tags/v" + version;
    }
    return scriptVersionTag;
  }

  // todo: finalise if we need this
  //  public void triggerUpdate(OidcDetails oidcDetails, String version)
  //      throws IOException, InterruptedException {
  //    String versionToUpdateTo = version;
  //    System.out.println("version to update to:"+ versionToUpdateTo);
  //    JsonElement release;
  //    if (version == null) {
  //      release = getLastRelease();
  //      versionToUpdateTo = getReleaseVersion(release);
  //      System.out.println("version is null");
  //    } else {
  //      release = getReleaseByVersion(version);
  //      System.out.println("get release!");
  //    }
  //    downloadUpdateScript(release);
  //    downloadArmadilloJar(versionToUpdateTo);
  //    updateApplicationConfig(oidcDetails);
  //    runRestartScriptInDifferentThread(versionToUpdateTo, true);
  //    // pass new config
  //    // trigger script for stopping and restarting
  //    // make script (try to adjust existing), make sure it will work on current PR, but will
  // usually
  //    // use latest release
  //    // warning when major release
  //  }

  private boolean fileExistsInDir(String filename, String directory) throws IOException {
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

  private String getJarFromVersion(String version) {
    return String.format(ARMADILLO_JAR, version.replace("v", ""));
  }

  // Simple mutable state carrier — private inner class or a record (Java 16+)
  private static class ConfigParseState {
    boolean providerFound, providerMolgenisFound;
    boolean registrationFound, registrationMolgenisFound;
    boolean resourceServerFound, resourceServerJwtFound, resourceServerOpaqueFound;
    boolean clientIdUpdated, clientSecretUpdated;
    boolean issuerUriUpdated, deviceIssuerUriUpdated, deviceClientIdUpdated;
  }

  public boolean isArmadilloUpdateAvailable() throws IOException, InterruptedException {
    JsonElement lastRelease = getLastRelease();
    String lastVersion = getReleaseVersion(lastRelease);
    String armadilloJar = getJarFromVersion(lastVersion);
    return !fileExistsInDir(armadilloJar, armadilloHome);
  }

  private Instant getInstantFromDateTime(LocalDateTime dateTime) {
    ZoneId zone = ZoneId.systemDefault();
    ZoneOffset zoneOffSet = zone.getRules().getOffset(dateTime);
    return Instant.from(dateTime.atOffset(zoneOffSet));
  }

  private void downloadUpdateScript(JsonElement release) {
    String version = getReleaseVersion(release);
    String scriptVersionTag = getScriptVersionTag(version);
    String armadilloUpdateScriptUrl =
        String.format(UPDATE_SCRIPT_URL, scriptVersionTag, updateScript);
    String updateScriptPath = format("%s/%s", armadilloHome, updateScript);
    try {
      if (fileExistsInDir(updateScript, armadilloHome)) {
        Path path = Paths.get(updateScriptPath);
        Instant creationDateInstant = getCreationDateOfFile(path).toInstant();
        Instant releaseDateInstant = getInstantFromDateTime(getLastReleaseDate(release));
        if (creationDateInstant.isBefore(releaseDateInstant)) {
          downloadFile(format(armadilloUpdateScriptUrl, version), updateScriptPath);
        }
      } else {
        downloadFile(format(armadilloUpdateScriptUrl, version), updateScriptPath);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, e.getMessage() + ": directory doesn't exist.");
    }
  }

  private FileTime getCreationDateOfFile(Path filePath) throws FileNotFoundException {
    try {
      return (FileTime) Files.getAttribute(filePath, CREATION_TIME);
    } catch (IOException ex) {
      throw new FileNotFoundException(filePath + NOT_FOUND);
    }
  }

  private String getJarHome() {
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
    SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout
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
    return emitter;
  }

  public void saveNewOidcConfig(OidcDetails oidcDetails) throws FileNotFoundException {
    updateApplicationConfig(oidcDetails);
    hardRestartApplication();
  }

  private void downloadFile(String url, String outputFile) {
    downloadFile(url, outputFile, progress -> {});
  }

  private void downloadFile(String url, String outputFile, LongConsumer progressCallback) {
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

        byte[] dataBuffer = new byte[8192];
        int bytesRead;
        long totalRead = 0;
        long lastReportedPercent = -1;

        while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
          fileOutputStream.write(dataBuffer, 0, bytesRead);
          totalRead += bytesRead;

          if (fileSize > 0) {
            long percent = (totalRead * 100) / fileSize;
            if (percent != lastReportedPercent) {
              lastReportedPercent = percent;
              progressCallback.accept(percent);
            }
          } else {
            progressCallback.accept(totalRead);
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
