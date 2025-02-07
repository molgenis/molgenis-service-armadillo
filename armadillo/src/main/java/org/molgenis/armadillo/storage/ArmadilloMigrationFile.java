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

  String getMatch(Pattern pattern, String stringToMatch) {
    Matcher matcher = pattern.matcher(stringToMatch);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  String getMigrationStatus(String statusLine) {
    if (statusLine.startsWith("Successfully")) {
      return "success";
    } else if (statusLine.startsWith("Cannot")) {
      return "failure";
    } else {
      return "";
    }
  }

  HashMap<String, String> parseLine(String line) {
    HashMap<String, String> parsedLine = new HashMap<>();
    String status = getMigrationStatus(line);
    Pattern workspacePattern = Pattern.compile("workspace \\[(.+:.+\\.RData)] from");
    String workspace = getMatch(workspacePattern, line);
    Pattern oldUserPattern =
        Pattern.compile("\\[(user-[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12})]");
    String oldUser = getMatch(oldUserPattern, line);
    Pattern newUserPattern = Pattern.compile("to \\[(user-.+__at__.+\\.[a-z]{2,3})][,%][n ]");
    String newUser = getMatch(newUserPattern, line);
    Pattern errorMessagePattern = Pattern.compile(", because \\[(.+)]");
    String errorMessage = getMatch(errorMessagePattern, line);
    parsedLine.put("status", status);
    parsedLine.put("workspace", workspace);
    parsedLine.put("oldUserFolder", oldUser);
    parsedLine.put("newUserFolder", newUser);
    parsedLine.put("errorMessage", errorMessage);
    return !status.isEmpty() ? parsedLine : null;
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
