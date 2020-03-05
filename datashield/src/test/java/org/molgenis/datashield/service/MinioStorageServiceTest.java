package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import io.minio.MinioClient;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.MinioConfig;
import org.molgenis.datashield.exceptions.StorageException;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

  private MinioStorageService minioStorageService;
  @Mock private MinioClient minioClient;
  @Mock private MinioConfig minioConfig;
  @Mock private InputStream inputStream;

  @BeforeEach
  public void beforeEach() {
    when(minioConfig.getBucket()).thenReturn("bucket");
    minioStorageService = new MinioStorageService(minioClient, minioConfig);
  }

  @Test
  public void testCheckBucketExistsThrowsExceptionIfMinioDown() throws Exception {
    doThrow(new IOException("blah")).when(minioClient).bucketExists("bucket");

    assertThrows(StorageException.class, minioStorageService::checkBucketExists);
  }

  @Test
  public void testCheckBucketExistsCreatesBucketIfNotFound() throws Exception {
    when(minioClient.bucketExists("bucket")).thenReturn(false);

    minioStorageService.checkBucketExists();

    verify(minioClient).makeBucket("bucket");
  }

  @Test
  public void save() throws Exception {
    minioStorageService.save(inputStream, "asdf.blah", APPLICATION_OCTET_STREAM);

    verify(minioClient)
        .putObject(
            "bucket", "asdf.blah", inputStream, null, null, null, APPLICATION_OCTET_STREAM_VALUE);
  }

  @Test
  public void saveThrowsException() throws Exception {
    IOException exception = new IOException("blah");
    doThrow(exception)
        .when(minioClient)
        .putObject(
            "bucket", "asdf.blah", inputStream, null, null, null, APPLICATION_OCTET_STREAM_VALUE);

    assertThrows(
        StorageException.class,
        () -> minioStorageService.save(inputStream, "asdf.blah", APPLICATION_OCTET_STREAM));
  }
}
