package org.molgenis.armadillo.storage;

import static java.lang.String.format;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmadilloMigrationFile {
  static final String MIGRATION_FILE_NAME = "migration-status";
  static final String MIGRATION_FILE_EXTENSION = ".amf";

  Path migrationFilePath;

  public ArmadilloMigrationFile(String rootDir, String filePath) {
    Path bucketPath = Paths.get(rootDir, filePath).toAbsolutePath().normalize();
    migrationFilePath =
        Paths.get(bucketPath + File.separator + MIGRATION_FILE_NAME + MIGRATION_FILE_EXTENSION);
  }

  public List<HashMap<String, String>> getMigrationStatus() throws FileNotFoundException {
    File migrationFile = migrationFilePath.toFile();
    Scanner reader = new Scanner(migrationFile);
    ArrayList<HashMap<String, String>> migrationStatus = new ArrayList<>();
    try {
      while (reader.hasNextLine()) {
        String data = reader.nextLine();
        if (!Objects.equals(data, "\n")) {
          HashMap<String, String> parsedLine = parseLine(data);
          if (!parsedLine.isEmpty()) {
            migrationStatus.add(parsedLine);
          }
        }
      }
    } finally {
      reader.close();
    }
    return migrationStatus;
  }

  HashMap<String, String> parseLine(String line) {
    HashMap<String, String> parsedLine = new HashMap<>();
    Pattern pattern = Pattern.compile("");
    String status = "";
    if (line.startsWith("Successfully")) {
      pattern =
          Pattern.compile(
              "Successfully migrated workspace \\[([a-zA-Z0-9_-]+:[a-zA-Z0-9._-]+\\.RData)] from \\[user-[a-zA-Z0-9-]+] to \\[user-[a-zA-Z0-9.-]+(?:__at__)?[a-zA-Z0-9.-]+]\n");
      status = "success";
    } else if (line.startsWith("Cannot migrate workspace")) {
      pattern =
          Pattern.compile(
              "Cannot migrate workspace \\[([a-zA-Z0-9_-]+:[a-zA-Z0-9._-]+\\.RData)] from \\[user-[a-zA-Z0-9-]+] to \\[user-[a-zA-Z0-9.-]+(?:__at__)?[a-zA-Z0-9.-]+], because \\[([^\\]]+)]. Workspace needs to be moved manually.\n");
      status = "failure";
    }
    Matcher matcher = pattern.matcher(line);
    while (matcher.find()) {
      if (!status.isEmpty()) {
        parsedLine.put("workspace", matcher.group(1));
        parsedLine.put("oldUserFolder", matcher.group(2));
        parsedLine.put("newUserFolder", matcher.group(3));
        parsedLine.put("status", status);
      }
      if (status.equals("failure")) {
        parsedLine.put("errorMessage", matcher.group(4));
      }
    }
    return parsedLine;
  }

  String getMigrationSuccessMessage(
      String workspaceName, String oldBucketName, String newBucketName) {
    return format(
        "Successfully migrated workspace [%s] from [%s] to [%s]%n",
        workspaceName, oldBucketName, newBucketName);
  }

  String getMigrationFailureMessage(
      String workspaceName, String oldBucketName, String newBucketName, String errorMessage) {
    return format(
        "Cannot migrate workspace [%s] from [%s] to [%s], because [%s]. Workspace needs to be moved manually.%n",
        workspaceName, oldBucketName, newBucketName, errorMessage);
  }

  void addLine(String line) throws IOException {
    if (Files.exists(migrationFilePath)) {
      Files.write(migrationFilePath, line.getBytes(), StandardOpenOption.APPEND);
    } else {
      Files.write(migrationFilePath, line.getBytes(), StandardOpenOption.CREATE);
    }
  }
}
