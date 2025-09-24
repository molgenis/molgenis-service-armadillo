package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.armadillo.exceptions.IllegalPathException;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.model.ArmadilloColumnMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

  static final String ROOT_DIR_PROPERTY = "storage.root-dir";

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorageService.class);

  public final String rootDir;

  public String getRootDir() {
    return rootDir;
  }

  public LocalStorageService(@Value("${" + ROOT_DIR_PROPERTY + "}") String rootDir) {
    var dir = new File(rootDir);
    if (!dir.isDirectory()) {
      throw new StorageException(
          format(
              "Unable to start LocalStorageService - %s: %s is not a directory",
              ROOT_DIR_PROPERTY, dir.getAbsolutePath()));
    }

    this.rootDir = rootDir;

    LOGGER.info("Using local storage at {}", dir.getAbsolutePath());
  }

  @Override
  public List<String> getUnavailableVariables(
      String bucketName, String objectName, String variables) throws IOException {
    Path objectPath = getPathIfObjectExists(bucketName, objectName + PARQUET);
    List<String> availableColumns = ParquetUtils.getColumns(objectPath);
    return Arrays.stream(variables.split(","))
        .filter(variable -> !availableColumns.contains(variable))
        .toList();
  }

  @Override
  public boolean objectExists(String bucketName, String objectName) {
    Objects.requireNonNull(objectName);

    try {
      // check bucket
      if (!bucketExists(bucketName)) {
        return false;
      }
      // check object
      Path object = getObjectPathSafely(bucketName, objectName);
      return Files.exists(object);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  public boolean bucketExists(String bucketName) {
    // check bucket
    Path dir = Paths.get(rootDir, bucketName);
    return Files.exists(dir);
  }

  @Override
  public void createBucketIfNotExists(String bucketName) {
    try {
      Path path = Paths.get(rootDir, bucketName);
      if (!Files.exists(path)) {
        Files.createDirectory(path);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void deleteBucket(String bucketName) {
    Path path = Paths.get(rootDir, bucketName);
    try (var folder = Files.walk(path)) {
      //noinspection ResultOfMethodCallIgnored
      folder.map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<String> listBuckets() {
    var files = new File(rootDir).listFiles();
    if (files == null) {
      return emptyList();
    }
    return Arrays.stream(files).filter(File::isDirectory).map(File::getName).toList();
  }

  @Override
  public void save(
      InputStream inputStream, String bucketName, String objectName, MediaType mediaType) {
    Path path = getObjectPathSafely(bucketName, objectName);
    try {
      createBucketIfNotExists(bucketName);

      // create parent dirs if needed
      //noinspection ResultOfMethodCallIgnored
      path.toFile().getParentFile().mkdirs();

      try (FileOutputStream outputStream = new FileOutputStream(path.toFile(), false)) {
        inputStream.transferTo(outputStream);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  /** Detects path traversal attacks. */
  Path getObjectPathSafely(String bucketName, String objectName) {
    Path path = Paths.get(rootDir, bucketName, objectName).toAbsolutePath().normalize();
    Path rootPath = Paths.get(rootDir, bucketName).toAbsolutePath().normalize();

    if (!path.startsWith(rootPath)) {
      throw new IllegalPathException(objectName);
    }

    return path;
  }

  @Override
  public List<ObjectMetadata> listObjects(String bucketName) {
    try {
      Path bucketPath = Paths.get(rootDir, bucketName);
      if (!Files.exists(bucketPath)) {
        return emptyList();
      } else {
        try (var files = Files.walk(bucketPath)) {
          return files
              .filter(Files::isRegularFile)
              .map(objectPath -> ObjectMetadata.of(bucketPath, objectPath))
              .toList();
        }
      }
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public InputStream load(String bucketName, String objectName) {
    try {
      Objects.requireNonNull(bucketName);
      Objects.requireNonNull(objectName);

      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      return new FileInputStream(objectPath.toFile());
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  public static String getFileSizeInUnit(long fileSize) {
    int sizeOfUnit = 1024;
    String[] units = new String[] {"bytes", "KB", "MB", "GB"};
    for (String unit : units) {
      if (fileSize > sizeOfUnit) {
        fileSize = fileSize / sizeOfUnit;
      } else {
        return fileSize + " " + unit;
      }
    }
    return fileSize + " " + units[units.length - 1];
  }

  @Override
  public FileInfo getInfo(String bucketName, String objectName) {
    try {
      Objects.requireNonNull(bucketName);
      Objects.requireNonNull(objectName);

      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      String objectPathString = objectPath.toString().toLowerCase();
      long fileSize = Files.size(objectPath);
      String fileSizeWithUnit = getFileSizeInUnit(fileSize);
      if (objectPathString.endsWith(PARQUET)) {
        Map<String, String> tableDimensions = ParquetUtils.retrieveDimensions(objectPath);
        return FileInfo.of(
            objectName,
            fileSizeWithUnit,
            tableDimensions.get("rows"),
            tableDimensions.get("columns"));
      } else if (objectPathString.endsWith(LINK_FILE)) {
        return getFileInfoForLinkFile(bucketName, objectName, fileSizeWithUnit);
      } else {
        return FileInfo.of(objectName, fileSizeWithUnit);
      }
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  ArmadilloLinkFile getArmadilloLinkFileFromName(String bucketName, String objectName) {
    InputStream armadilloLinkFileStream = load(bucketName, objectName);
    return new ArmadilloLinkFile(armadilloLinkFileStream, bucketName, objectName);
  }

  public ArmadilloWorkspace getWorkSpace(InputStream is) {
    return new ArmadilloWorkspace(is);
  }

  Map<String, ArmadilloColumnMetaData> getMetaDataForLinkfile(String bucketName, String objectName)
      throws IOException {
    ArmadilloLinkFile linkFile = getArmadilloLinkFileFromName(bucketName, objectName);
    List<String> columns = Arrays.asList(linkFile.getVariables().split(","));
    Path srcObjectPath =
        getPathIfObjectExists(
            SHARED_PREFIX + linkFile.getSourceProject(), linkFile.getSourceObject() + PARQUET);
    Map<String, ArmadilloColumnMetaData> metadata = ParquetUtils.getColumnMetaData(srcObjectPath);
    return metadata.entrySet().stream()
        .filter(
            map -> {
              String key = map.getKey();
              return columns.contains(key);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, ArmadilloColumnMetaData> getMetadataFromTablePath(
      String bucketName, String objectName) {
    try {
      Objects.requireNonNull(bucketName);
      Objects.requireNonNull(objectName);
      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      if (objectPath.toString().endsWith(PARQUET)) {
        return ParquetUtils.getColumnMetaData(objectPath);
      } else if (objectPath.toString().endsWith(LINK_FILE)) {
        return getMetaDataForLinkfile(bucketName, objectName);
      } else {
        throw new StorageException(
            format(
                "Object [%s/%s] is not a table, cannot determine metadata.",
                bucketName, objectName));
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  FileInfo getFileInfoForLinkFile(String bucketName, String objectName, String fileSizeWithUnit)
      throws FileNotFoundException {
    ArmadilloLinkFile linkFile = getArmadilloLinkFileFromName(bucketName, objectName);
    Path srcObjectPath =
        getPathIfObjectExists(
            SHARED_PREFIX + linkFile.getSourceProject(), linkFile.getSourceObject() + PARQUET);
    Map<String, String> tableDimensions = ParquetUtils.retrieveDimensions(srcObjectPath);
    return new FileInfo(
        objectName,
        fileSizeWithUnit,
        tableDimensions.get("rows"),
        String.valueOf(linkFile.getNumberOfVariables()),
        linkFile.getSourceProject() + "/" + linkFile.getSourceObject(),
        linkFile.getVariables().split(","));
  }

  @Override
  public List<String> getVariables(String bucketName, String objectName) {
    try {
      Objects.requireNonNull(bucketName);
      Objects.requireNonNull(objectName);

      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      return ParquetUtils.getColumns(objectPath);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<Map<String, String>> preview(
      String bucketName, String objectName, int rowLimit, int columnLimit) {
    try {
      Objects.requireNonNull(bucketName);
      Objects.requireNonNull(objectName);

      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      if (objectPath.toString().endsWith(PARQUET)) {
        return ParquetUtils.previewRecords(objectPath, rowLimit, columnLimit, new String[0]);
      } else if (objectPath.toString().endsWith(LINK_FILE)) {
        ArmadilloLinkFile linkFile = getArmadilloLinkFileFromName(bucketName, objectName);
        String srcProject = linkFile.getSourceProject();
        String srcObject = linkFile.getSourceObject();
        String[] variables = linkFile.getVariables().split(",");
        Path srcObjectPath = getPathIfObjectExists(SHARED_PREFIX + srcProject, srcObject + PARQUET);
        return ParquetUtils.previewRecords(srcObjectPath, rowLimit, columnLimit, variables);
      } else {
        throw new StorageException(
            format("Preview not supported for: %s/%s", bucketName, objectName));
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  public Path getPathIfObjectExists(String bucketName, String objectName) {
    Path bucketPath = Paths.get(rootDir, bucketName);
    if (!Files.exists(bucketPath)) {
      throw new StorageException(format("Bucket '%s' doesn't exist", bucketName));
    }
    Path objectPath = getObjectPathSafely(bucketName, objectName);
    if (!Files.exists(objectPath)) {
      throw new StorageException(
          format("Object '%s' doesn't exist in bucket '%s'", objectName, bucketName));
    }
    return objectPath;
  }

  @Override
  public void delete(String bucketName, String objectName) {
    Objects.requireNonNull(bucketName);
    Objects.requireNonNull(objectName);

    try {
      Path objectPath = getPathIfObjectExists(bucketName, objectName);
      Files.delete(objectPath);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }
}
