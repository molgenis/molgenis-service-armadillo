package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.StorageException;

class StorageServiceTest {

  @Test
  void testProjectName() {
    assertDoesNotThrow(() -> StorageService.validateProjectName("lifecycle"));
  }

  @Test
  void testProjectNameUppercase() {
    var exception =
        assertThrows(StorageException.class, () -> StorageService.validateProjectName("Lifecycle"));
    assertEquals("Project names cannot contain uppercase characters", exception.getMessage());
  }

  @Test
  void testProjectNameNull() {
    assertThrows(NullPointerException.class, () -> StorageService.validateProjectName(null));
  }
}
