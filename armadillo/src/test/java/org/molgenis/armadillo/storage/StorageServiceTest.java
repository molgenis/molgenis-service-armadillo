package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.StorageException;

class StorageServiceTest {

  @Test
  void testProjectName() {
    assertDoesNotThrow(() -> StorageService.validateBucketName("lifecycle"));
  }

  @Test
  void testProjectNameUppercase() {
    var exception =
        assertThrows(StorageException.class, () -> StorageService.validateBucketName("Lifecycle"));
    assertEquals("Project names cannot contain uppercase characters", exception.getMessage());
  }

  @Test
  void testProjectNameNull() {
    assertThrows(NullPointerException.class, () -> StorageService.validateBucketName(null));
  }
}
