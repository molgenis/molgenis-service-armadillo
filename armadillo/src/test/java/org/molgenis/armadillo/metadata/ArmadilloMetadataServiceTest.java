package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
  void testBootstrapAdmin() {
    var metadataService = new ArmadilloMetadataService(storage, loader, "bofke@gmail.com", null);
    metadataService.initialize();

    assertEquals(Boolean.TRUE, metadataService.userByEmail("bofke@gmail.com").getAdmin());
  }

  @Test
  void testBootstrapDefaultProject() {
    var metadataService = new ArmadilloMetadataService(storage, loader, null, "test");
    metadataService.initialize();

    assertNotNull(metadataService.projectsByName("test"));
  }

  @Test
  void testBootstrapAll() {
    var metadataService = new ArmadilloMetadataService(storage, loader, "bofke@gmail.com", "test");
    metadataService.initialize();

    assertEquals(Boolean.TRUE, metadataService.userByEmail("bofke@gmail.com").getAdmin());
    assertNotNull(metadataService.projectsByName("test"));
  }
}
