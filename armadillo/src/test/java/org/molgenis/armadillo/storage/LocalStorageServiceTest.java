package org.molgenis.armadillo.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.StorageException;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class LocalStorageServiceTest {

    public static final String SOME_OBJECT_PATH = "object/some/path"; //n.b. can be subfolders you see?
    public static final String SOME_BUCKET = "bucket";
    LocalStorageService localStorageService;

    @BeforeEach
    void beforeEach() throws IOException {
        String tmpDir = Files.createTempDirectory("armadilloStorageTest").toFile().getAbsolutePath();
        localStorageService = new LocalStorageService(tmpDir);
    }

    @Test
    void testCheckBucketExistsCreatesBucketIfNotFound() {
        localStorageService.createBucketIfNotExists(SOME_BUCKET);
        assertTrue(localStorageService.listBuckets().contains(SOME_BUCKET));
    }

    @Test
    void testCheckObjectExistsChecksExistenceNoSuchObject() throws Exception {
        assertFalse(localStorageService.objectExists(SOME_BUCKET, SOME_OBJECT_PATH));
    }

    @Test
    void testCheckObjectExistsInvalidBucketname() throws Exception {
        assertThrows(
                StorageException.class, () -> localStorageService.objectExists("Bucket", SOME_OBJECT_PATH));
    }

    @Test
    void testCheckObjectExistsChecksExistenceObjectExists() throws Exception {
        //create some file
        localStorageService.save(new ByteArrayInputStream("test".getBytes()), SOME_BUCKET, SOME_OBJECT_PATH, MediaType.TEXT_PLAIN);
        //test it exists
        assertTrue(localStorageService.objectExists(SOME_BUCKET, SOME_OBJECT_PATH));
        //test it has expected metadata
        ObjectMetadata metadata = localStorageService.listObjects(SOME_BUCKET).get(0);
        assertTrue(metadata.getLastModified().before(new Date()));
        assertTrue(metadata.getSize() > 0);
    }

    @Test
    void save() throws Exception {
        //create some file
        localStorageService.save(new ByteArrayInputStream("test".getBytes()), SOME_BUCKET, SOME_OBJECT_PATH, MediaType.TEXT_PLAIN);

        //check it exists
        assertTrue(localStorageService.objectExists(SOME_BUCKET, SOME_OBJECT_PATH));
    }


    @Test
    void testListWorkspacesNoBucket() {
        assertEquals(Collections.emptyList(), localStorageService.listObjects("user-admin"));
    }

    @Test
    void testLoad() throws Exception {
        //write a file
        localStorageService.save(new ByteArrayInputStream("test".getBytes()), "user-admin", "blah.RData", MediaType.TEXT_PLAIN);
        //compare the contents
        assertTrue(Arrays.equals(new ByteArrayInputStream("test".getBytes()).readAllBytes(), localStorageService.load("user-admin", "blah.RData").readAllBytes()));
    }

    @Test
    void testDelete() throws Exception {
        //write a file
        localStorageService.save(new ByteArrayInputStream("test".getBytes()), "user-admin", "blah.RData", MediaType.TEXT_PLAIN);

        //check it exists
        assertTrue(localStorageService.objectExists("user-admin", "blah.RData"));

        //delete a file
        localStorageService.delete("user-admin", "blah.RData");

        //check removed
        assertFalse(localStorageService.objectExists("user-admin", "blah.RData"));
    }
}
