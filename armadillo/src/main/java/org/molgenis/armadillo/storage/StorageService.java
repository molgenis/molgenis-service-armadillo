package org.molgenis.armadillo.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;

public interface StorageService {
  boolean objectExists(String bucket, String objectName);

  void createBucketIfNotExists(String bucketName);

  void deleteBucket(String bucketName);

  List<String> listBuckets();

  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listObjects(String bucketName);

  InputStream load(String bucketName, String objectName);

  List<Map<String, String>> preview(
      String bucketName, String objectName, int rowLimit, int columnLimit);

  void delete(String bucketName, String objectName);

  public Path getPathIfObjectExists(String bucketName, String objectName);
}
