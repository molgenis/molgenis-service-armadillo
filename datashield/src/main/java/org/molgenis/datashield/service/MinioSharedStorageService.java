package org.molgenis.datashield.service;

import com.google.common.collect.Streams;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.molgenis.datashield.MinioSharedStorageConfig;
import org.molgenis.datashield.exceptions.StorageException;
import org.molgenis.datashield.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.xmlpull.v1.XmlPullParserException;

@Service("sharedStorageService")
public class MinioSharedStorageService implements StorageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MinioSharedStorageService.class);

  private final MinioClient sharedStorageClient;
  private final String bucketName;

  public MinioSharedStorageService(
      MinioClient sharedStorageClient, MinioSharedStorageConfig minioConfig) {
    this.sharedStorageClient = sharedStorageClient;
    this.bucketName = minioConfig.getBucket();
  }

  @PostConstruct
  public void checkBucketExists() {
    try {
      if (!sharedStorageClient.bucketExists(bucketName)) {
        sharedStorageClient.makeBucket(bucketName);
        LOGGER.info("Created bucket {}.", bucketName);
      }
      LOGGER.debug("Storing data in bucket {}.", bucketName);
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
  public void save(InputStream is, String objectName, MediaType mediaType) {
    try {
      LOGGER.info("Putting object {}.", objectName);
      sharedStorageClient.putObject(
          bucketName, objectName, is, null, null, null, mediaType.toString());
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
  public InputStream load(String objectName) {
    try {
      LOGGER.info("Getting object {}.", objectName);
      return sharedStorageClient.getObject(bucketName, objectName);
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
  public void delete(String objectName) {
    try {
      LOGGER.info("Deleting object {}.", objectName);
      sharedStorageClient.removeObject(bucketName, objectName);
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
  public List<Workspace> listWorkspaces(String prefix) {
    try {
      LOGGER.info("List objects {}.", prefix);
      return Streams.stream(sharedStorageClient.listObjects(bucketName, prefix))
          .map(MinioSharedStorageService::toWorkspace)
          .map(it -> it.trim(prefix, ".RData"))
          .collect(Collectors.toList());
    } catch (XmlPullParserException e) {
      throw new StorageException(e);
    }
  }
}
