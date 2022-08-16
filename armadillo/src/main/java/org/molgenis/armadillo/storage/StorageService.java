package org.molgenis.armadillo.storage;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.List;
import org.molgenis.armadillo.exceptions.StorageException;
import org.springframework.http.MediaType;

public interface StorageService {
  boolean bucketExists(String bucket, String objectName);

  void createBucketIfNotExists(String bucketName);

  void deleteBucket(String bucketName);

  List<String> listBuckets();

  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listBuckets(String bucketName);

  InputStream load(String bucketName, String objectName);

  void delete(String bucketName, String objectName);

  static void validateBucketName(String bucketName) {
    requireNonNull(bucketName);

    if (!bucketName.toLowerCase().equals(bucketName)) {
      throw new StorageException("Bucket names cannot contain uppercase characters");
    }
  }
}
