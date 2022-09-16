package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.storage.ArmadilloStorageService;

@ExtendWith(MockitoExtension.class)
class ArmadilloMetadataServiceTest {

  @Mock private ArmadilloStorageService storage;

  private final MetadataLoader loader = new DummyMetadataLoader();

  @Test
  void test() {
    var metadataService = new ArmadilloMetadataService(storage, loader, "bofke@gmail.com");
    metadataService.initialize();

    assertEquals(Boolean.TRUE, metadataService.userByEmail("bofke@gmail.com").getAdmin());
  }
}
