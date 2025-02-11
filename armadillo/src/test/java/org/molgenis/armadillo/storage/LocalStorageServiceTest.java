package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.storage.StorageService.getHumanReadableByteCount;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.molgenis.armadillo.exceptions.IllegalPathException;
import org.molgenis.armadillo.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class LocalStorageServiceTest {
  @Autowired LocalStorageService localStorageService;
  public static final String SOME_OBJECT_PATH =
      "object/some/path"; // n.b. can be subfolders you see?
  public static final String SOME_PROJECT = "project";
  public static final String WORKSPACE_NAME = "workspace1";

  @Mock Principal principal;

  @Mock ObjectMetadata workspaceMetaData;

  @BeforeEach
  void beforeEach() throws IOException {
    String tmpDir = Files.createTempDirectory("armadilloStorageTest").toFile().getAbsolutePath();
    localStorageService = new LocalStorageService(tmpDir);
  }

  @Test
  void testCheckProjectExistsCreatesProjectIfNotFound() {
    localStorageService.createBucketIfNotExists(SOME_PROJECT);
    assertTrue(localStorageService.listBuckets().contains(SOME_PROJECT));
  }

  @Test
  void testCheckObjectExistsChecksExistenceNoSuchObject() {
    assertFalse(localStorageService.objectExists(SOME_PROJECT, SOME_OBJECT_PATH));
  }

  @Test
  void testCheckObjectExistsChecksExistenceObjectExists() {
    // create some file
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()),
        SOME_PROJECT,
        SOME_OBJECT_PATH,
        MediaType.TEXT_PLAIN);
    // test it exists
    assertTrue(localStorageService.objectExists(SOME_PROJECT, SOME_OBJECT_PATH));
    // test it has expected metadata
    ObjectMetadata metadata = localStorageService.listObjects(SOME_PROJECT).get(0);
    assertTrue(
        metadata
            .lastModified()
            .isBefore(ZonedDateTime.from(Instant.now().atZone(ZoneId.systemDefault()))));
    assertTrue(metadata.size() > 0);
  }

  @Test
  void save() {
    // create some file
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()),
        SOME_PROJECT,
        SOME_OBJECT_PATH,
        MediaType.TEXT_PLAIN);

    // check it exists
    assertTrue(localStorageService.objectExists(SOME_PROJECT, SOME_OBJECT_PATH));
  }

  @Test
  void testListWorkspacesNoProject() {
    assertEquals(Collections.emptyList(), localStorageService.listObjects("user-admin"));
  }

  @Test
  void testLoad() throws Exception {
    // write a file
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()),
        "user-admin",
        "blah.RData",
        MediaType.TEXT_PLAIN);
    // compare the contents
    assertArrayEquals(
        new ByteArrayInputStream("test".getBytes()).readAllBytes(),
        localStorageService.load("user-admin", "blah.RData").readAllBytes());
  }

  @Test
  void testDelete() {
    // write a file
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()),
        "user-admin",
        "blah.RData",
        MediaType.TEXT_PLAIN);

    // check it exists
    assertTrue(localStorageService.objectExists("user-admin", "blah.RData"));

    // delete a file
    localStorageService.delete("user-admin", "blah.RData");

    // check removed
    assertFalse(localStorageService.objectExists("user-admin", "blah.RData"));
  }

  @Test
  void testDeleteBucket() {
    localStorageService.createBucketIfNotExists("delete-bucket-test");

    assertTrue(
        localStorageService.listBuckets().stream()
            .anyMatch(bucket -> bucket.equals("delete-bucket-test")));

    localStorageService.deleteBucket("delete-bucket-test");

    assertTrue(
        localStorageService.listBuckets().stream()
            .noneMatch(bucket -> bucket.equals("delete-bucket-test")));
  }

  @Test
  void testGetObjectPathSafely() {
    assertDoesNotThrow(
        () -> localStorageService.getObjectPathSafely("test", "core/malicious.parquet"));
  }

  @Test
  void testGetObjectPathSafelyMalicious() {
    assertThrows(
        IllegalPathException.class,
        () -> localStorageService.getObjectPathSafely("test", "../../malicious.parquet"));
  }

  @Test
  void testGetFileSizeInUnitBytes() {
    assertEquals("10 bytes", localStorageService.getFileSizeInUnit(10));
  }

  @Test
  void testGetFileSizeInUnitKb() {
    assertEquals("10 KB", localStorageService.getFileSizeInUnit(10240));
  }

  @Test
  void testGetFileSizeInUnitMb() {
    assertEquals("10 MB", localStorageService.getFileSizeInUnit(10485760));
  }

  @Test
  void testGetFileSizeInUnitGb() {
    assertEquals("10 GB", localStorageService.getFileSizeInUnit(10737418240L));
  }

  @Test
  void testGetPathIfObjectExists() {
    String bucket = "my-bucket";
    String object = "my-object.txt";
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class, RETURNS_DEEP_STUBS);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    Path bucketPathMock = mock(Path.class);
    Path objectPathMock = mock(Path.class);
    when(Paths.get(localStorageService.rootDir, bucket, object).toAbsolutePath().normalize())
        .thenReturn(objectPathMock);
    when(Paths.get(localStorageService.rootDir, bucket)).thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath())
        .thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath().normalize())
        .thenReturn(bucketPathMock);
    when(objectPathMock.startsWith(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(objectPathMock)).thenReturn(Boolean.TRUE);
    assertEquals(objectPathMock, localStorageService.getPathIfObjectExists(bucket, object));
    mockedPaths.close();
    mockedFiles.close();
  }

  @Test
  void testGetUnavailableVariables() throws IOException {
    String bucket = "my-bucket";
    String object = "my-object";
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class, RETURNS_DEEP_STUBS);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    MockedStatic<ParquetUtils> mockedParquetUtils = Mockito.mockStatic(ParquetUtils.class);
    Path bucketPathMock = mock(Path.class);
    Path objectPathMock = mock(Path.class);
    when(Paths.get(localStorageService.rootDir, bucket, object + ".parquet")
            .toAbsolutePath()
            .normalize())
        .thenReturn(objectPathMock);
    when(Paths.get(localStorageService.rootDir, bucket)).thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath())
        .thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath().normalize())
        .thenReturn(bucketPathMock);
    when(objectPathMock.startsWith(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(objectPathMock)).thenReturn(Boolean.TRUE);
    when(ParquetUtils.getColumns(objectPathMock)).thenReturn(List.of("var1", "var2"));
    List<String> unavailableVariables =
        localStorageService.getUnavailableVariables(bucket, object, "var1,var2,var3");
    assertEquals(unavailableVariables, List.of("var3"));
    mockedPaths.close();
    mockedFiles.close();
    mockedParquetUtils.close();
  }

  @Test
  void testGetPathIfObjectExistsThrowsErrorWhenNoBucket() {
    String object = "my-object.txt";
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    Path bucketPathMock = mock(Path.class);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.FALSE);
    assertThrows(
        StorageException.class, () -> localStorageService.getPathIfObjectExists(null, object));
    mockedPaths.close();
    mockedFiles.close();
  }

  @Test
  void testGetPathIfObjectExistsThrowsErrorWhenNoObject() {
    String bucket = "my-bucket";
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class, RETURNS_DEEP_STUBS);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    Path bucketPathMock = mock(Path.class);
    Path objectPathMock = mock(Path.class);
    when(Paths.get(localStorageService.rootDir, bucket, null).toAbsolutePath().normalize())
        .thenReturn(objectPathMock);
    when(Paths.get(localStorageService.rootDir, bucket)).thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath())
        .thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath().normalize())
        .thenReturn(bucketPathMock);
    when(objectPathMock.startsWith(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(objectPathMock)).thenReturn(Boolean.FALSE);
    assertThrows(
        StorageException.class, () -> localStorageService.getPathIfObjectExists(bucket, null));
    mockedPaths.close();
    mockedFiles.close();
  }

  @Test
  void testGetInfoForTable() throws IOException {
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class, RETURNS_DEEP_STUBS);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    MockedStatic<ParquetUtils> mockedParquetUtils = Mockito.mockStatic(ParquetUtils.class);
    String bucket = "my-bucket";
    String object = "my-object.parquet";
    Path bucketPathMock = mock(Path.class);
    Path objectPathMock = mock(Path.class);
    HashMap<String, String> objectDimensions =
        new HashMap<>() {
          {
            put("rows", "232000");
            put("columns", "120");
          }
        };
    when(Paths.get(localStorageService.rootDir, bucket, object).toAbsolutePath().normalize())
        .thenReturn(objectPathMock);
    when(Paths.get(localStorageService.rootDir, bucket)).thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath())
        .thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath().normalize())
        .thenReturn(bucketPathMock);
    when(objectPathMock.startsWith(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(objectPathMock)).thenReturn(Boolean.TRUE);
    when(objectPathMock.toString()).thenReturn(bucket + " " + object);
    when(Files.size(objectPathMock)).thenReturn(10485760L);
    when(ParquetUtils.retrieveDimensions(objectPathMock)).thenReturn(objectDimensions);
    FileInfo expected = new FileInfo(object, "10 MB", "232000", "120", null, new String[] {});
    assertEquals(expected, localStorageService.getInfo(bucket, object));
    mockedPaths.close();
    mockedFiles.close();
    mockedParquetUtils.close();
  }

  @Test
  void testGetInfoForNonTable() throws IOException {
    MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class, RETURNS_DEEP_STUBS);
    MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
    MockedStatic<ParquetUtils> mockedParquetUtils = Mockito.mockStatic(ParquetUtils.class);
    String bucket = "my-bucket";
    String object = "my-object.rda";
    Path bucketPathMock = mock(Path.class);
    Path objectPathMock = mock(Path.class);
    when(Paths.get(localStorageService.rootDir, bucket, object).toAbsolutePath().normalize())
        .thenReturn(objectPathMock);
    when(Paths.get(localStorageService.rootDir, bucket)).thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath())
        .thenReturn(bucketPathMock);
    when(Paths.get(localStorageService.rootDir, bucket).toAbsolutePath().normalize())
        .thenReturn(bucketPathMock);
    when(objectPathMock.startsWith(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(bucketPathMock)).thenReturn(Boolean.TRUE);
    when(Files.exists(objectPathMock)).thenReturn(Boolean.TRUE);
    when(objectPathMock.toString()).thenReturn(bucket + " " + object);
    when(Files.size(objectPathMock)).thenReturn(10737418240L);
    FileInfo expected = new FileInfo(object, "10 GB", null, null, null, new String[] {});
    assertEquals(expected, localStorageService.getInfo(bucket, object));
    mockedPaths.close();
    mockedFiles.close();
    mockedParquetUtils.close();
  }

  @Test
  void testGetVariables() {
    String bucket = "bucket";
    String object = "table.parquet";
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()), bucket, object, MediaType.TEXT_PLAIN);
    MockedStatic<ParquetUtils> mockedParquetUtils = Mockito.mockStatic(ParquetUtils.class);
    localStorageService.getVariables(bucket, object);
    Path path = localStorageService.getObjectPathSafely(bucket, object);
    mockedParquetUtils.verify(() -> ParquetUtils.getColumns(path));
    mockedParquetUtils.close();
  }

  @Test
  void testPreview() {
    String bucket = "bucket";
    String object = "table.parquet";
    localStorageService.save(
        new ByteArrayInputStream("test".getBytes()), bucket, object, MediaType.TEXT_PLAIN);
    MockedStatic<ParquetUtils> mockedParquetUtils = Mockito.mockStatic(ParquetUtils.class);
    localStorageService.preview(bucket, object, 10, 10);
    Path path = localStorageService.getObjectPathSafely(bucket, object);
    mockedParquetUtils.verify(() -> ParquetUtils.previewRecords(path, 10, 10, new String[0]));
    mockedParquetUtils.close();
  }

  @Test
  void testGetHumanReadableByteCountKb() {
    String size = getHumanReadableByteCount(1234);
    assertEquals("1.2 KB", size);
  }

  @Test
  void testGetHumanReadableByteCountMb() {
    String size = getHumanReadableByteCount(12345678);
    assertEquals("11.8 MB", size);
  }

  @Test
  void testGetHumanReadableByteCountGb() {
    String size = getHumanReadableByteCount(12345678910L);
    assertEquals("11.5 GB", size);
  }

  @Test
  void testMoveWorkspace() {
    // Setup test data: Create a workspace in the old bucket
    String oldBucketName = "old-bucket";
    String newBucketName = "new-bucket";

    localStorageService.createBucketIfNotExists(oldBucketName);

    // Create a workspace in the old bucket
    localStorageService.save(
        new ByteArrayInputStream("workspace content".getBytes()),
        oldBucketName,
        WORKSPACE_NAME,
        MediaType.APPLICATION_OCTET_STREAM);

    // Initialize the mock ObjectMetadata and define its behavior
    workspaceMetaData = mock(ObjectMetadata.class); // Ensure workspaceMetaData is not null
    when(workspaceMetaData.name()).thenReturn(WORKSPACE_NAME); // Mock name() method

    // Call the moveWorkspace method
    localStorageService.moveWorkspace(workspaceMetaData, principal, oldBucketName, newBucketName);

    // Verify both workspaces are there (we don't want to remove the old one in case not everything
    // is moved)
    assertTrue(localStorageService.objectExists(newBucketName, WORKSPACE_NAME));
    assertTrue(localStorageService.objectExists(oldBucketName, WORKSPACE_NAME));
  }

  @Test
  void testMoveWorkspaceFileDoesNotExistInOldBucket() {
    // Setup test data: Create workspace in old bucket
    String oldBucketName = "old-bucket";
    String newBucketName = "new-bucket";

    localStorageService.createBucketIfNotExists(newBucketName);

    // Initialize the mock ObjectMetadata and define its behavior
    workspaceMetaData = mock(ObjectMetadata.class); // Ensure workspaceMetaData is not null
    when(workspaceMetaData.name()).thenReturn(WORKSPACE_NAME); // Mock name() method

    assertThrows(
        StorageException.class,
        () ->
            localStorageService.moveWorkspace(
                workspaceMetaData, principal, oldBucketName, newBucketName));
  }
}
