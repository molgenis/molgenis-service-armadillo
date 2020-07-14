package org.molgenis.armadillo.service;

import java.io.InputStream;
import java.util.List;
import org.molgenis.armadillo.model.Workspace;
import org.springframework.http.MediaType;

/** Reads and writes data to storage. */
public interface StorageService {
  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  InputStream load(String bucketName, String objectName);

  void delete(String bucketName, String objectName);

  List<Workspace> listWorkspaces(String bucketName);
}
