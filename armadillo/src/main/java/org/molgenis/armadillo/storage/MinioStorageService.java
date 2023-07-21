package org.molgenis.armadillo.storage;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.molgenis.armadillo.storage.MinioStorageService.MINIO_URL_PROPERTY;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Bucket;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(MINIO_URL_PROPERTY)
class MinioStorageService implements StorageService {

  static final String MINIO_URL_PROPERTY = "minio.url";
  static final String ERROR_NO_SUCH_KEY = "NoSuchKey";
  static final String ERROR_NO_SUCH_OBJECT = "NoSuchObject";
  static final String ERROR_NO_SUCH_BUCKET = "NoSuchBucket";
  static final int PART_SIZE = 10 * 1024 * 1024;

  private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageService.class);

  private final MinioClient minioClient;

  public MinioStorageService(MinioClient minioClient) {
    this.minioClient = minioClient;

    LOGGER.info("Using MinIO as storage");
  }

  @Override
  public boolean objectExists(String bucket, String objectName) {
    try {
      minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build());
      return true;
    } catch (ErrorResponseException error) {
      var code = error.errorResponse().code();
      if (code.equals(ERROR_NO_SUCH_KEY)
          || code.equals(ERROR_NO_SUCH_OBJECT)
          || code.equals(ERROR_NO_SUCH_BUCKET)) {
        return false;
      } else {
        throw new StorageException(error);
      }
    } catch (NoSuchAlgorithmException
        | InvalidResponseException
        | InternalException
        | InvalidKeyException
        | IOException
        | InsufficientDataException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void createBucketIfNotExists(String projectName) {
    try {
      if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(projectName).build())) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(projectName).build());
        LOGGER.info("Created bucket {}.", projectName);
      }
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | InvalidResponseException
        | ErrorResponseException
        | InternalException
        | IOException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void deleteBucket(String projectName) {
    try {
      LOGGER.info("Deleting bucket {}.", projectName);
      minioClient.removeBucket(RemoveBucketArgs.builder().bucket(projectName).build());
    } catch (InternalException
        | InvalidResponseException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | ErrorResponseException
        | InsufficientDataException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<String> listBuckets() {
    try {
      return minioClient.listBuckets().stream().map(Bucket::name).toList();
    } catch (NoSuchAlgorithmException
        | InsufficientDataException
        | InvalidResponseException
        | InternalException
        | ErrorResponseException
        | InvalidKeyException
        | IOException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void save(InputStream is, String projectName, String objectName, MediaType mediaType) {
    createBucketIfNotExists(projectName);
    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            format("Putting object %s in bucket %s.", objectName, projectName)
                .replaceAll("[\n\r\t]", "_"));
      }
      minioClient.putObject(
          PutObjectArgs.builder().bucket(projectName).object(objectName).stream(is, -1, PART_SIZE)
              .contentType(mediaType.toString())
              .build());
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | InvalidResponseException
        | ErrorResponseException
        | InternalException
        | IOException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public List<ObjectMetadata> listObjects(String projectName) {
    try {
      LOGGER.info("List objects in bucket {}.", projectName);
      List<ObjectMetadata> result = newArrayList();
      for (var itemResult :
          minioClient.listObjects(ListObjectsArgs.builder().bucket(projectName).build())) {
        var item = itemResult.get();
        result.add(ObjectMetadata.of(item));
      }
      return result;
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | ErrorResponseException
        | InternalException
        | IOException
        | InvalidResponseException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public InputStream load(String projectName, String objectName) {
    try {
      LOGGER.info("Getting object {}.", objectName);
      return minioClient.getObject(
          GetObjectArgs.builder().bucket(projectName).object(objectName).build());
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | InvalidResponseException
        | ErrorResponseException
        | InternalException
        | IOException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public Map<String, String> getInfo(String bucketName, String objectName) {
    return null;
  }

  @Override
  public List<Map<String, String>> preview(
      String bucketName, String objectName, int rowLimit, int columnLimit) {
    throw new UnsupportedOperationException();
    // would require us I suppose to download the file to temp and then run the ParquetUtils.preview
    // from there
  }

  @Override
  public void delete(String projectName, String objectName) {
    try {
      LOGGER.info("Deleting object {}.", objectName);
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(projectName).object(objectName).build());
    } catch (InvalidKeyException
        | InsufficientDataException
        | NoSuchAlgorithmException
        | InvalidResponseException
        | ErrorResponseException
        | InternalException
        | IOException
        | ServerException
        | XmlParserException e) {
      throw new StorageException(e);
    }
  }
}
