package org.molgenis.armadillo.storage;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.storage.MinioStorageService.ERROR_NO_SUCH_KEY;
import static org.molgenis.armadillo.storage.MinioStorageService.ERROR_NO_SUCH_OBJECT;
import static org.molgenis.armadillo.storage.MinioStorageService.PART_SIZE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  @BeforeEach
  void beforeEach() {
    minioStorageService = new MinioStorageService(minioClient);
  }

  @Test
  void testCheckProjectExistsThrowsExceptionIfMinioDown() throws Exception {
    doThrow(new IOException("blah"))
        .when(minioClient)
        .bucketExists(BucketExistsArgs.builder().bucket("project").build());

    assertThrows(
        StorageException.class, () -> minioStorageService.createBucketIfNotExists("project"));
  }

  @Test
  void testCheckProjectExistsCreatesProjectIfNotFound() throws Exception {
    when(minioClient.bucketExists(BucketExistsArgs.builder().bucket("project").build()))
        .thenReturn(false);

    minioStorageService.createBucketIfNotExists("project");

    verify(minioClient).makeBucket(MakeBucketArgs.builder().bucket("project").build());
  }

  @Test
  void testCheckObjectExistsChecksExistenceNoSuchKey() throws Exception {
    when(errorResponseException.errorResponse()).thenReturn(errorResponse);
    when(errorResponse.code()).thenReturn(ERROR_NO_SUCH_KEY);
    doThrow(errorResponseException)
        .when(minioClient)
        .statObject(StatObjectArgs.builder().bucket("project").object("object").build());

    assertFalse(minioStorageService.objectExists("project", "object"));
  }

  @Test
  void testCheckObjectExistsChecksExistenceNoSuchObject() throws Exception {
    when(errorResponseException.errorResponse()).thenReturn(errorResponse);
    when(errorResponse.code()).thenReturn(ERROR_NO_SUCH_OBJECT);
    doThrow(errorResponseException)
        .when(minioClient)
        .statObject(StatObjectArgs.builder().bucket("project").object("object").build());

    assertFalse(minioStorageService.objectExists("project", "object"));
  }

  @Test
  void testCheckObjectExistsChecksExistenceObjectExists() throws Exception {
    var statObjectResponse = mock(StatObjectResponse.class);
    when(minioClient.statObject(
            StatObjectArgs.builder().bucket("project").object("object").build()))
        .thenReturn(statObjectResponse);

    assertTrue(minioStorageService.objectExists("project", "object"));
  }

  @Test
  void save() throws Exception {
    minioStorageService.save(inputStream, "project", "asdf.blah", APPLICATION_OCTET_STREAM);
    var captor = ArgumentCaptor.forClass(PutObjectArgs.class);

    verify(minioClient).putObject(captor.capture());
    var value = captor.getValue();
    assertEquals(APPLICATION_OCTET_STREAM_VALUE, value.contentType());
    assertEquals("project", value.bucket());
    assertEquals("asdf.blah", value.object());
    assertEquals(PART_SIZE, value.partSize());
    assertEquals(-1, value.objectSize());
  }

  @Test
  void saveThrowsException() throws Exception {
    IOException exception = new IOException("blah");
    doThrow(exception).when(minioClient).putObject(any(PutObjectArgs.class));

    assertThrows(
        StorageException.class,
        () ->
            minioStorageService.save(
                inputStream, "project", "asdf.blah", APPLICATION_OCTET_STREAM));
  }

  @Test
  void testListWorkspacesNoProject() {
    assertEquals(emptyList(), minioStorageService.listObjects("user-admin"));
  }

  @Test
  void testLoad() throws Exception {
    var getObjectResponse = mock(GetObjectResponse.class);
    when(minioClient.getObject(
            GetObjectArgs.builder().bucket("user-admin").object("blah.RData").build()))
        .thenReturn(getObjectResponse);

    assertSame(getObjectResponse, minioStorageService.load("user-admin", "blah.RData"));
  }

  @Test
  void testDelete() throws Exception {
    minioStorageService.delete("user-admin", "blah.RData");

    verify(minioClient)
        .removeObject(RemoveObjectArgs.builder().bucket("user-admin").object("blah.RData").build());
  }

  @Test
  void testDeleteBucket() throws Exception {
    minioStorageService.deleteBucket("test");

    verify(minioClient).removeBucket(RemoveBucketArgs.builder().bucket("test").build());
  }
}
