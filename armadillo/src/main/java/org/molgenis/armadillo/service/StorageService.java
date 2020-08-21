package org.molgenis.armadillo.service;

import io.minio.messages.Bucket;
import io.minio.messages.Item;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.MediaType;

/** Reads and writes data to storage. */
public interface StorageService {

  List<Bucket> listBuckets();

  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  List<Item> listObjects(String bucketName);

  InputStream load(String bucketName, String objectName);

  void delete(String bucketName, String objectName);
}
