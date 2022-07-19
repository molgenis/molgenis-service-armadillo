package org.molgenis.armadillo.storage;

import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    boolean objectExists(String bucket, String objectName);

    void createBucketIfNotExists(String bucket);

    List<String> listBuckets();

    void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

    List<ObjectMetadata> listObjects(String bucketName);

    InputStream load(String bucketName, String objectName);

    void delete(String bucketName, String objectName);
}
