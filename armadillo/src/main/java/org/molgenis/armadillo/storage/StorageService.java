package org.molgenis.armadillo.storage;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.List;
import org.molgenis.armadillo.exceptions.StorageException;
import org.springframework.http.MediaType;

public interface StorageService {
  boolean objectExists(String bucket, String objectName);

  void createProjectIfNotExists(String projectName);

  List<String> listProjects();

  void save(InputStream is, String projectName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listObjects(String projectName);

  InputStream load(String projectName, String objectName);

  void delete(String projectName, String objectName);

  static void validateProjectName(String projectName) {
    requireNonNull(projectName);

    if (!projectName.toLowerCase().equals(projectName)) {
      throw new StorageException("Project names cannot contain uppercase characters");
    }
  }
}
