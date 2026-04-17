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
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.springframework.beans.factory.annotation.Value;
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

  // DEV/PROD
  @Value("${armadillo.armadillo-mode:PROD}")
  String armadilloMode;

  // Constants
  String updateScript = "armadillo-check-update.sh";
  String RELEASE_URL = "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases";
  String UPDATE_SCRIPT_URL =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/refs/tags/%s/scripts/install/%s";
  String RELEASE_DOWNLOAD_URL =
      "https://github.com/molgenis/molgenis-service-armadillo/releases/download/%s/%s";
  String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";
  String PRERELEASE = "prerelease";
  String FALSE = "false";
  String PUBLISHED_AT = "published_at";
  String TAG = "tag_name";
  String BACKUP_EXT = ".bak";
  String PROGRESS = "progress";
  String DONE = "done";
  String CREATION_TIME = "creationTime";
  String DOWNLOAD_COMPLETE = "Download complete";
  String NOT_FOUND = "not found";
  String PROVIDER = "provider:";
  String REGISTRATION = "registration:";
  String RESOURCE_SERVER = "resourceserver:";
  String MOLGENIS = "molgenis:";
  String CLIENT_SECRET = "client-secret:";
  String CLIENT_ID = "client-id:";
  String ISSUER_URI = "issuer-uri:";
  String OPAQUE_TOKEN = "opaquetoken:";
  String JWT = "jwt:";

  public ManagementService() {}

  public void restartApplication() {
    ArmadilloServiceApplication.restart();
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
            .filter(release -> ((JsonObject) release).get(PRERELEASE).getAsString().equals(FALSE))
            .findFirst();
    return lastRelease.get();
  }

  private LocalDateTime getLastReleaseDate(JsonElement lastRelease) {
    String rawDate = ((JsonObject) lastRelease).get(PUBLISHED_AT).getAsString();
    DateTimeFormatter inputFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    return LocalDateTime.parse(rawDate, inputFormatter);
  }

  public String getReleaseVersion(JsonElement release) {
    return ((JsonObject) release).get(TAG).getAsString();
  }

  public void triggerUpdate(OidcDetails oidcDetails) throws IOException, InterruptedException {
    JsonElement lastRelease = getLastRelease();
    String lastVersion = getReleaseVersion(lastRelease);
    // todo: replace current update script with one we can use
    downloadUpdateScript();
    downloadArmadilloJar(lastVersion);
    updateApplicationConfig(oidcDetails);
    // pass new config
    // trigger script for stopping and restarting
    // make script (try to adjust existing), make sure it will work on current PR, but will usually
    // use latest release
    // warning when major release
  }

  private boolean fileExistsInDir(String filename, String directory) throws IOException {
    Set<String> foundFiles = listFilesForDir(directory);
    return foundFiles.contains(filename);
  }

  private void updateApplicationConfig(OidcDetails oidcDetails) throws FileNotFoundException {
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

    if (line.contains(PROVIDER)) {
      state.providerFound = true;
      return line;
    }
    if (line.contains(REGISTRATION)) {
      state.registrationFound = true;
      return line;
    }
    if (line.contains(RESOURCE_SERVER)) {
      state.resourceServerFound = true;
      return line;
    }
    if (line.contains(JWT) && state.resourceServerFound) {
      state.resourceServerJwtFound = true;
      return line;
    }
    if (line.contains(OPAQUE_TOKEN) && state.resourceServerFound) {
      state.resourceServerOpaqueFound = true;
      return line;
    }

    if (line.contains(MOLGENIS)) {
      if (state.providerFound) state.providerMolgenisFound = true;
      if (state.registrationFound) state.registrationMolgenisFound = true;
      return line;
    }

    if (line.contains(ISSUER_URI)) {
      if (state.providerMolgenisFound && !state.issuerUriUpdated) {
        state.issuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getIssuerUri());
      }
      if (state.resourceServerJwtFound && !state.deviceIssuerUriUpdated) {
        state.deviceIssuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceIssuerUri());
      }
    }

    if (line.contains(CLIENT_ID)) {
      if (state.registrationMolgenisFound && !state.clientIdUpdated) {
        state.clientIdUpdated = true;
        return replaceValue(line, oidcDetails.getClientId());
      }
      if (state.resourceServerOpaqueFound && !state.deviceClientIdUpdated) {
        state.deviceClientIdUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceClientId());
      }
    }

    if (line.contains(CLIENT_SECRET)
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
    File armadilloJar = new File(armadilloHome + "/" + getJarFromVersion(version));
    // todo: make sure it's not the jar we're using
    if (armadilloJar.delete()) {
      // todo: log
      System.out.println("Deleted the file: " + armadilloJar.getName());
    } else {
      throw new StorageException("Cannot delete file: " + armadilloJar.getName());
    }
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

  private void downloadUpdateScript() throws IOException, InterruptedException {
    JsonElement lastRelease = getLastRelease();
    String lastVersion = getReleaseVersion(lastRelease);
    String armadilloUpdateScriptUrl = String.format(UPDATE_SCRIPT_URL, lastVersion, updateScript);
    String updateScriptPath = format("%s/%s", armadilloHome, updateScript);
    try {
      if (fileExistsInDir(updateScript, armadilloHome)) {
        // todo: LOG INFO update script already found!
        Path path = Paths.get(updateScriptPath);
        Instant creationDateInstant = getCreationDateOfFile(path).toInstant();
        Instant releaseDateInstant = getInstantFromDateTime(getLastReleaseDate(lastRelease));
        if (creationDateInstant.isBefore(releaseDateInstant)) {
          downloadFile(format(armadilloUpdateScriptUrl, lastVersion), updateScriptPath);
        }
      } else {
        downloadFile(format(armadilloUpdateScriptUrl, lastVersion), updateScriptPath);
      }
      // todo: download anyway if below 5.13
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

  public Set<String> listAvailableJars() {
    return listFilesForDir(armadilloHome).stream()
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
    String armadilloInstallation = format("%s/%s", armadilloHome, armadilloJar);

    // Run download in background thread — SSE must not block the request thread
    Thread.ofVirtual()
        .start(
            () -> {
              try {
                if (fileExistsInDir(armadilloJar, armadilloHome)) {
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
    restartApplication();
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
