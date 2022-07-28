package org.molgenis.armadillo.storage;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.molgenis.armadillo.exceptions.StorageException;
import org.springframework.http.MediaType;

public class LocalStorageService implements StorageService {

  private final String rootDir;

  public LocalStorageService(String rootDir) {
    Objects.requireNonNull(rootDir);

    this.rootDir = rootDir;
  }

  @Override
  public boolean objectExists(String bucket, String objectName) {
    Objects.requireNonNull(bucket);
    Objects.requireNonNull(objectName);

    // no uppercase in bucket name
    if (bucket.matches(".*[A-Z].*")) {
      // unsure why this matters
      throw new StorageException("Buckets cannot contain uppercase characters");
    }
    try {
      // check bucket
      Path dir = Paths.get(rootDir, bucket);
      if (!Files.exists(dir)) {
        return false;
      }
      // check object
      Path object = Paths.get(rootDir, bucket, objectName);
      return Files.exists(object);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void createBucketIfNotExists(String bucket) {
    Objects.requireNonNull(bucket);
    try {
      Path path = Paths.get(rootDir, bucket);
      if (!Files.exists(path)) {
        Files.createDirectory(path);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<String> listBuckets() {
    var files = new File(rootDir).listFiles();
    if (files == null) return emptyList();
    return Arrays.stream(files)
        .filter(File::isDirectory)
        .map(File::getName)
        .collect(Collectors.toList());
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
        return Files.list(bucketPath)
            .map(Path::toFile)
            .map(ObjectMetadata::of)
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
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
      throw new StorageException(String.format("Bucket '%s' doesn't exist", bucketName));
    }
    Path objectPath = Paths.get(rootDir, bucketName, objectName);
    if (!Files.exists(objectPath)) {
      throw new StorageException(
          String.format("Object '%s' doesn't exist in bucket '%s'", bucketName, objectName));
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
