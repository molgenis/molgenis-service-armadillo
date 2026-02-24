package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.DefaultImageMetadata;

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
}
