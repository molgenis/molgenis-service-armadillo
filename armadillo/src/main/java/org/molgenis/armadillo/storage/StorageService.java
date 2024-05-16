package org.molgenis.armadillo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;

public interface StorageService {
  boolean objectExists(String bucket, String objectName);

  List<String> getUnavailableVariables(String bucketName, String objectName, String variables)
      throws IOException;

  void createBucketIfNotExists(String bucketName);

  void deleteBucket(String bucketName);

  List<String> listBuckets();

  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listObjects(String bucketName);

  InputStream load(String bucketName, String objectName);

  FileInfo getInfo(String bucketName, String objectName);

  List<String> getVariables(String bucketName, String objectName);

  List<Map<String, String>> preview(
      String bucketName, String objectName, int rowLimit, int columnLimit);

  void delete(String bucketName, String objectName);

  Path getPathIfObjectExists(String bucketName, String objectName);

  long getSizeOfInputStream(InputStream is) throws IOException;
}
