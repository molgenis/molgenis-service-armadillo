package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;

@ExtendWith(MockitoExtension.class)
class ArmadilloMetadataServiceTest {

  @Mock ArmadilloStorageService storage;

  @Test
  void test() {
    when(storage.loadSystemFile(METADATA_FILE))
        .thenThrow(new StorageException("no system file found"));

    var metadataService = new ArmadilloMetadataService(storage, "bofke@gmail.com");

    assertEquals(Boolean.TRUE, metadataService.userByEmail("bofke@gmail.com").getAdmin());
  }
}
