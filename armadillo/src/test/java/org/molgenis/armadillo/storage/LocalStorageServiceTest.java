package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.IllegalPathException;
import org.springframework.http.MediaType;

class LocalStorageServiceTest {

  public static final String SOME_OBJECT_PATH =
      "object/some/path"; // n.b. can be subfolders you see?
  public static final String SOME_PROJECT = "project";
  LocalStorageService localStorageService;

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
}
