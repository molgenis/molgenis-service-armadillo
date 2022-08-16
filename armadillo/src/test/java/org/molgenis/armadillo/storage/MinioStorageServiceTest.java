package org.molgenis.armadillo.storage;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import io.minio.ErrorCode;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.StorageException;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

  private MinioStorageService minioStorageService;
  @Mock private MinioClient minioClient;
  @Mock private InputStream inputStream;
  @Mock private ErrorResponseException errorResponseException;
  @Mock private ErrorResponse errorResponse;
  @Mock private ObjectStat objectStat;

  @BeforeEach
  void beforeEach() {
    minioStorageService = new MinioStorageService(minioClient);
  }

  @Test
  void testCheckProjectExistsThrowsExceptionIfMinioDown() throws Exception {
    doThrow(new IOException("blah")).when(minioClient).bucketExists("project");

    assertThrows(
        StorageException.class, () -> minioStorageService.createBucketIfNotExists("project"));
  }

  @Test
  void testCheckProjectExistsCreatesProjectIfNotFound() throws Exception {
    when(minioClient.bucketExists("project")).thenReturn(false);

    minioStorageService.createBucketIfNotExists("project");

    verify(minioClient).makeBucket("project");
  }

  @Test
  void testCheckObjectExistsChecksExistenceNoSuchKey() throws Exception {
    when(errorResponseException.errorResponse()).thenReturn(errorResponse);
    when(errorResponse.errorCode()).thenReturn(ErrorCode.NO_SUCH_KEY);
    doThrow(errorResponseException).when(minioClient).statObject("project", "object");

    assertFalse(minioStorageService.bucketExists("project", "object"));
  }

  @Test
  void testCheckObjectExistsChecksExistenceNoSuchObject() throws Exception {
    when(errorResponseException.errorResponse()).thenReturn(errorResponse);
    when(errorResponse.errorCode()).thenReturn(ErrorCode.NO_SUCH_OBJECT);
    doThrow(errorResponseException).when(minioClient).statObject("project", "object");

    assertFalse(minioStorageService.bucketExists("project", "object"));
  }

  @Test
  void testCheckObjectExistsInvalidProjectname() throws Exception {
    doThrow(new InvalidBucketNameException("Project", "no capitals in bucket name!"))
        .when(minioClient)
        .statObject("Project", "object");

    assertThrows(
        StorageException.class, () -> minioStorageService.bucketExists("Project", "object"));
  }

  @Test
  void testCheckObjectExistsChecksExistenceObjectExists() throws Exception {
    when(minioClient.statObject("project", "object")).thenReturn(objectStat);

    assertTrue(minioStorageService.bucketExists("project", "object"));
  }

  @Test
  void save() throws Exception {
    minioStorageService.save(inputStream, "project", "asdf.blah", APPLICATION_OCTET_STREAM);

    verify(minioClient)
        .putObject(
            "project", "asdf.blah", inputStream, null, null, null, APPLICATION_OCTET_STREAM_VALUE);
  }

  @Test
  void saveThrowsException() throws Exception {
    IOException exception = new IOException("blah");
    doThrow(exception)
        .when(minioClient)
        .putObject(
            "project", "asdf.blah", inputStream, null, null, null, APPLICATION_OCTET_STREAM_VALUE);

    assertThrows(
        StorageException.class,
        () ->
            minioStorageService.save(
                inputStream, "project", "asdf.blah", APPLICATION_OCTET_STREAM));
  }

  @Test
  void testListWorkspacesNoProject() {
    assertEquals(emptyList(), minioStorageService.listBuckets("user-admin"));
  }

  @Test
  void testLoad() throws Exception {
    when(minioClient.getObject("user-admin", "blah.RData")).thenReturn(inputStream);

    assertSame(inputStream, minioStorageService.load("user-admin", "blah.RData"));
  }

  @Test
  void testDelete() throws Exception {
    minioStorageService.delete("user-admin", "blah.RData");

    verify(minioClient).removeObject("user-admin", "blah.RData");
  }

  @Test
  void testDeleteBucket() throws Exception {
    minioStorageService.deleteBucket("test");

    verify(minioClient).removeBucket("test");
  }
}
