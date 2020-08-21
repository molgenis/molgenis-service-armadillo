package org.molgenis.armadillo.minio;

import static com.google.common.collect.Lists.newArrayList;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.xmlpull.v1.XmlPullParserException;

@Component
public class MinioStorageService implements StorageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageService.class);

  private final MinioClient minioClient;

  public MinioStorageService(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

  @PreAuthorize("hasPermission(#bucket, 'load')")
  void checkBucketExists(String bucket) {
    try {
      if (!minioClient.bucketExists(bucket)) {
        minioClient.makeBucket(bucket);
        LOGGER.info("Created bucket {}.", bucket);
      }
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | NoResponseException
        | InvalidResponseException
        | XmlPullParserException
        | InvalidBucketNameException
        | ErrorResponseException
        | InternalException
        | IOException
        | RegionConflictException e) {
      throw new StorageException(e);
    }
  }

  @Override
  @PostFilter("hasPermission(filterObject, 'load')")
  public List<Bucket> listBuckets() {
    try {
      return minioClient.listBuckets();
    } catch (InvalidBucketNameException
        | NoSuchAlgorithmException
        | InsufficientDataException
        | InvalidResponseException
        | InternalException
        | ErrorResponseException
        | XmlPullParserException
        | NoResponseException
        | InvalidKeyException
        | IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void save(InputStream is, String bucketName, String objectName, MediaType mediaType) {
    checkBucketExists(bucketName);
    try {
      LOGGER.info("Putting object {} in bucket {}.", objectName, bucketName);
      minioClient.putObject(bucketName, objectName, is, null, null, null, mediaType.toString());
    } catch (InvalidKeyException
        | InvalidArgumentException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | NoResponseException
        | InvalidResponseException
        | XmlPullParserException
        | InvalidBucketNameException
        | ErrorResponseException
        | InternalException
        | IOException e) {
      throw new StorageException(e);
    }
  }

  @PreAuthorize("hasPermission(#bucketName, 'Bucket', 'load')")
  @Override
  public List<Item> listObjects(String bucketName) {
    try {
      LOGGER.info("List objects in bucket {}.", bucketName);
      List<Item> result = newArrayList();
      for (var itemResult : minioClient.listObjects(bucketName)) {
        var item = itemResult.get();
        result.add(item);
      }
      return result;
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | NoResponseException
        | XmlPullParserException
        | InvalidBucketNameException
        | ErrorResponseException
        | InternalException
        | IOException e) {
      throw new StorageException(e);
    }
  }

  //  @PreAuthorize("hasPermission(#bucketName, 'Bucket', 'load')")
  @Override
  public InputStream load(String bucketName, String objectName) {
    try {
      LOGGER.info("Getting object {}.", objectName);
      return minioClient.getObject(bucketName, objectName);
    } catch (InvalidKeyException
        | InvalidArgumentException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | NoResponseException
        | InvalidResponseException
        | XmlPullParserException
        | InvalidBucketNameException
        | ErrorResponseException
        | InternalException
        | IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void delete(String bucketName, String objectName) {
    try {
      LOGGER.info("Deleting object {}.", objectName);
      minioClient.removeObject(bucketName, objectName);
    } catch (InvalidKeyException
        | InvalidArgumentException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | NoResponseException
        | InvalidResponseException
        | XmlPullParserException
        | InvalidBucketNameException
        | ErrorResponseException
        | InternalException
        | IOException e) {
      throw new StorageException(e);
    }
  }
}
