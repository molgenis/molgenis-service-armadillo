package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.molgenis.armadillo.storage.LocalStorageService.ROOT_DIR_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.molgenis.armadillo.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(ROOT_DIR_PROPERTY)
public class LocalStorageService implements StorageService {

  static final String ROOT_DIR_PROPERTY = "storage.root-dir";

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorageService.class);

  private final String rootDir;

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
  public boolean bucketExists(String bucketName, String objectName) {
    Objects.requireNonNull(objectName);

    try {
      // check bucket
      Path dir = Paths.get(rootDir, bucketName);
      if (!Files.exists(dir)) {
        return false;
      }
      // check object
      Path object = Paths.get(rootDir, bucketName, objectName);
      return Files.exists(object);
    } catch (Exception e) {
      throw new StorageException(e);
    }
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
    Path path = Paths.get(rootDir, bucketName, objectName);
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

  private Path getPathIfObjectExists(String bucketName, String objectName) {
    Path bucketPath = Paths.get(rootDir, bucketName);
    if (!Files.exists(bucketPath)) {
      throw new StorageException(format("Bucket '%s' doesn't exist", bucketName));
    }
    Path objectPath = Paths.get(rootDir, bucketName, objectName);
    if (!Files.exists(objectPath)) {
      throw new StorageException(
          format("Object '%s' doesn't exist in bucket '%s'", bucketName, objectName));
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
