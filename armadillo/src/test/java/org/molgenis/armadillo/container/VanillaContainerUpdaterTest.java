package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.DefaultImageMetadata;
import org.molgenis.armadillo.metadata.OpenContainersImageMetadata;

class VanillaContainerUpdaterTest {

  @Test
  void updateDefaultImageMetadata_updatesFields() {
    VanillaContainerConfig existing =
        VanillaContainerConfig.builder().name("c1").installDate("old").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img1", 123L, "new");

    VanillaContainerUpdater updater = new VanillaContainerUpdater();
    VanillaContainerConfig updated =
        (VanillaContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img1", updated.getLastImageId());
    assertEquals(123L, updated.getImageSize());
    assertEquals("new", updated.getInstallDate());
  }

  @Test
  void updateDefaultImageMetadata_keepsExistingInstallDateWhenNull() {
    VanillaContainerConfig existing =
        VanillaContainerConfig.builder().name("c1").installDate("keep").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img2", 456L, null);

    VanillaContainerUpdater updater = new VanillaContainerUpdater();
    VanillaContainerConfig updated =
        (VanillaContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img2", updated.getLastImageId());
    assertEquals(456L, updated.getImageSize());
    assertEquals("keep", updated.getInstallDate());
  }

  @Test
  void updateDefaultImageMetadata_allowsNullInstallDateWhenExistingNull() {
    VanillaContainerConfig existing = VanillaContainerConfig.builder().name("c1").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img3", 789L, null);

    VanillaContainerUpdater updater = new VanillaContainerUpdater();
    VanillaContainerConfig updated =
        (VanillaContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img3", updated.getLastImageId());
    assertEquals(789L, updated.getImageSize());
    assertNull(updated.getInstallDate());
  }

  @Test
  void updateOpenContainersMetaData_updatesFields() {
    VanillaContainerConfig existing = VanillaContainerConfig.builder().name("c1").build();
    OpenContainersImageMetadata metadata =
        new OpenContainersImageMetadata("v1.2.3", "2025-01-15T10:00:00Z");

    VanillaContainerUpdater updater = new VanillaContainerUpdater();
    VanillaContainerConfig updated =
        (VanillaContainerConfig) updater.updateOpenContainersMetaData(existing, metadata);

    assertEquals("v1.2.3", updated.getVersionId());
    assertEquals("2025-01-15T10:00:00Z", updated.getCreationDate());
  }

  @Test
  void updateOpenContainersMetaData_allowsNullValues() {
    VanillaContainerConfig existing =
        VanillaContainerConfig.builder()
            .name("c1")
            .versionId("old-version")
            .creationDate("old-date")
            .build();
    OpenContainersImageMetadata metadata = new OpenContainersImageMetadata(null, null);

    VanillaContainerUpdater updater = new VanillaContainerUpdater();
    VanillaContainerConfig updated =
        (VanillaContainerConfig) updater.updateOpenContainersMetaData(existing, metadata);

    assertNull(updated.getVersionId());
    assertNull(updated.getCreationDate());
  }
}
