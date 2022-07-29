package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.molgenis.armadillo.storage.LocalStorageService.ROOT_DIR_PROPERTY;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(ROOT_DIR_PROPERTY)
public class LocalStorageService implements StorageService {

  static final String ROOT_DIR_PROPERTY = "local-storage.root-dir";

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
  }

  @Override
  public boolean objectExists(String projectName, String objectName) {
    Objects.requireNonNull(objectName);
    Objects.requireNonNull(projectName);

    try {
      // check project
      Path dir = Paths.get(rootDir, projectName);
      if (!Files.exists(dir)) {
        return false;
      }
      // check object
      Path object = Paths.get(rootDir, projectName, objectName);
      return Files.exists(object);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void createProjectIfNotExists(String projectName) {
    StorageService.validateProjectName(projectName);

    try {
      Path path = Paths.get(rootDir, projectName);
      if (!Files.exists(path)) {
        Files.createDirectory(path);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<String> listProjects() {
    var files = new File(rootDir).listFiles();
    if (files == null) {
      return emptyList();
    }
    return Arrays.stream(files)
        .filter(File::isDirectory)
        .map(File::getName)
        .collect(Collectors.toList());
  }

  @Override
  public void save(
      InputStream inputStream, String projectName, String objectName, MediaType mediaType) {
    Path path = Paths.get(rootDir, projectName, objectName);
    try {
      createProjectIfNotExists(projectName);

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
  public List<ObjectMetadata> listObjects(String projectName) {
    try {
      Path projectPath = Paths.get(rootDir, projectName);
      if (!Files.exists(projectPath)) {
        return emptyList();
      } else {
        return Files.list(projectPath)
            .map(Path::toFile)
            .map(ObjectMetadata::of)
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  @Override
  public InputStream load(String projectName, String objectName) {
    try {
      Objects.requireNonNull(projectName);
      Objects.requireNonNull(objectName);

      Path objectPath = getPathIfObjectExists(projectName, objectName);
      return new FileInputStream(objectPath.toFile());
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }

  private Path getPathIfObjectExists(String projectName, String objectName) {
    Path projectPath = Paths.get(rootDir, projectName);
    if (!Files.exists(projectPath)) {
      throw new StorageException(format("Project '%s' doesn't exist", projectName));
    }
    Path objectPath = Paths.get(rootDir, projectName, objectName);
    if (!Files.exists(objectPath)) {
      throw new StorageException(
          format("Object '%s' doesn't exist in project '%s'", projectName, objectName));
    }
    return objectPath;
  }

  @Override
  public void delete(String projectName, String objectName) {
    Objects.requireNonNull(projectName);
    Objects.requireNonNull(objectName);

    try {
      Path objectPath = getPathIfObjectExists(projectName, objectName);
      Files.delete(objectPath);
    } catch (Exception e) {
      throw new StorageException(e);
    }
  }
}
