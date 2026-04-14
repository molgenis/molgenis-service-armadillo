package org.molgenis.armadillo.service;

import static java.lang.String.format;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {
  @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}")
  String armadilloHome;

  @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
  String armadilloConfigFile;

  String updateScript = "armadillo-check-update.sh";

  public ManagementService() {}

  public void restartApplication() {
    ArmadilloServiceApplication.restart();
  }

  private JsonArray getReleases() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases"))
            .GET()
            .build();
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

  private JsonElement getLastRelease() throws IOException, InterruptedException {
    JsonArray armadilloReleases = getReleases();
    Optional<JsonElement> lastRelease =
        StreamSupport.stream(armadilloReleases.spliterator(), false)
            .filter(
                release -> ((JsonObject) release).get("prerelease").getAsString().equals("false"))
            .findFirst();
    return lastRelease.get();
  }

  private LocalDateTime getLastReleaseDate(JsonElement lastRelease) {
    String rawDate = ((JsonObject) lastRelease).get("published_at").getAsString();
    DateTimeFormatter inputFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    return LocalDateTime.parse(rawDate, inputFormatter);
  }

  private String getLastReleaseVersion(JsonElement lastRelease) {
    return ((JsonObject) lastRelease).get("tag_name").getAsString();
  }

  public void triggerUpdate() throws IOException, InterruptedException {
    JsonElement lastRelease = getLastRelease();
    // todo: replace current update script with one we can use
    downloadUpdateScript(lastRelease);
    // todo: add possibility to use last prerelease as well
    downloadLatestArmadillo(lastRelease, armadilloHome);
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

  private void downloadLatestArmadillo(JsonElement lastRelease, String armadilloAppHome) {
    String lastVersion = getLastReleaseVersion(lastRelease);
    String armadilloJar = String.format("molgenis-armadillo-%s.jar", lastVersion.replace("v", ""));
    String downloadUrl =
        String.format(
            "https://github.com/molgenis/molgenis-service-armadillo/releases/download/%s/%s",
            lastVersion, armadilloJar);
    String armadilloInstallation = format("%s/%s", armadilloAppHome, armadilloJar);
    try {
      if (fileExistsInDir(armadilloJar, armadilloAppHome)) {
        // todo: LOG INFO update script already found!
      } else {
        downloadFile(format(downloadUrl, lastVersion), armadilloInstallation);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, e.getMessage() + ": directory doesn't exist.");
    }
  }

  private Instant getInstantFromDateTime(LocalDateTime dateTime) {
    ZoneId zone = ZoneId.systemDefault();
    ZoneOffset zoneOffSet = zone.getRules().getOffset(dateTime);
    return Instant.from(dateTime.atOffset(zoneOffSet));
  }

  private void downloadUpdateScript(JsonElement lastRelease) {
    String lastVersion = getLastReleaseVersion(lastRelease);
    String armadilloUpdateScriptUrl =
        String.format(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/refs/tags/%s/scripts/install/%s",
            lastVersion, updateScript);
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
      return (FileTime) Files.getAttribute(filePath, "creationTime");
    } catch (IOException ex) {
      throw new FileNotFoundException(filePath + "not found");
    }
  }

  private Set<String> listFilesForDir(String dir) throws IOException {
    return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  public void downloadFile(String url, String outputFile) {
    // todo: log download
    try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
      byte dataBuffer[] = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
        fileOutputStream.write(dataBuffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
