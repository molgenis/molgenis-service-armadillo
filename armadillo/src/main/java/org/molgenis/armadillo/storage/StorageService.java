package org.molgenis.armadillo.storage;

import java.io.InputStream;
import java.util.List;
import org.springframework.http.MediaType;

public interface StorageService {
  boolean objectExists(String bucket, String objectName);

  void createProjectIfNotExists(String projectName);

  List<String> listProjects();

  void save(InputStream is, String projectName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listObjects(String projectName);

  InputStream load(String projectName, String objectName);

  void delete(String projectName, String objectName);
}
