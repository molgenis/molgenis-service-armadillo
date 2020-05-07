package org.molgenis.armadillo.minio;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.model.Workspace;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

  private MinioStorageService minioStorageService;
  @Mock private MinioClient minioClient;
  @Mock private InputStream inputStream;
  @Mock private Result<Item> itemResult;
  @Mock private Item item;

  @BeforeEach
  public void beforeEach() {
    minioStorageService = new MinioStorageService(minioClient, "bucket");
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

  @Test
  public void testListWorkspaces() throws Exception {
    Instant lastModified = Instant.now().truncatedTo(MILLIS);
    Workspace workspace =
        Workspace.builder()
            .setName("blah")
            .setLastModified(lastModified)
            .setETag("\"abcde\"")
            .setSize(56)
            .build();

    when(minioClient.listObjects("bucket", "admin/")).thenReturn(List.of(itemResult));
    when(itemResult.get()).thenReturn(item);
    when(item.objectName()).thenReturn("admin/blah.RData");
    when(item.lastModified()).thenReturn(Date.from(lastModified));
    when(item.etag()).thenReturn(workspace.eTag());
    when(item.objectSize()).thenReturn(workspace.size());

    assertEquals(List.of(workspace), minioStorageService.listWorkspaces("admin/"));
  }

  @Test
  public void testLoad() throws Exception {
    when(minioClient.getObject("bucket", "admin/blah.RData")).thenReturn(inputStream);

    assertSame(inputStream, minioStorageService.load("admin/blah.RData"));
  }

  @Test
  public void testDelete() throws Exception {
    minioStorageService.delete("admin/blah.RData");

    verify(minioClient).removeObject("bucket", "admin/blah.RData");
  }
}
