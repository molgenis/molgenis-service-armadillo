package org.molgenis.armadillo.minio;

import static java.util.Collections.emptyList;

import com.google.common.collect.Streams;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.messages.Item;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.armadillo.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.xmlpull.v1.XmlPullParserException;

@Component
public class MinioStorageService implements StorageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageService.class);

  private final MinioClient minioClient;

  public MinioStorageService(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

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

  public static Workspace toWorkspace(Result<Item> result) {
    try {
      Item item = result.get();
      return Workspace.builder()
          .setLastModified(item.lastModified())
          .setName(item.objectName())
          .setSize(item.objectSize())
          .setETag(item.etag())
          .build();
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

  @Override
  public List<Workspace> listWorkspaces(String bucketName) {
    try {
      if (!minioClient.bucketExists(bucketName)) {
        return emptyList();
      }
      LOGGER.debug("List objects.");
      return Streams.stream(minioClient.listObjects(bucketName))
          .map(MinioStorageService::toWorkspace)
          .map(it -> it.trim("", ".RData"))
          .collect(Collectors.toList());
    } catch (XmlPullParserException
        | InvalidBucketNameException
        | NoSuchAlgorithmException
        | InsufficientDataException
        | IOException
        | InvalidKeyException
        | NoResponseException
        | ErrorResponseException
        | InternalException
        | InvalidResponseException e) {
      throw new StorageException(e);
    }
  }
}
