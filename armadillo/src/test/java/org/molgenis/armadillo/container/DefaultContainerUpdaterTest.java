package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.DefaultImageMetadata;

class DefaultContainerUpdaterTest {

  @Test
  void updateDefaultImageMetadata_updatesFields() {
    DefaultContainerConfig existing =
        DefaultContainerConfig.builder().name("c1").installDate("old").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img1", 123L, "new");

    DefaultContainerUpdater updater = new DefaultContainerUpdater();
    DefaultContainerConfig updated =
        (DefaultContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img1", updated.getLastImageId());
    assertEquals(123L, updated.getImageSize());
    assertEquals("new", updated.getInstallDate());
  }

  @Test
  void updateDefaultImageMetadata_keepsExistingInstallDateWhenNull() {
    DefaultContainerConfig existing =
        DefaultContainerConfig.builder().name("c1").installDate("keep").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img2", 456L, null);

    DefaultContainerUpdater updater = new DefaultContainerUpdater();
    DefaultContainerConfig updated =
        (DefaultContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img2", updated.getLastImageId());
    assertEquals(456L, updated.getImageSize());
    assertEquals("keep", updated.getInstallDate());
  }

  @Test
  void updateDefaultImageMetadata_allowsNullInstallDateWhenExistingNull() {
    DefaultContainerConfig existing = DefaultContainerConfig.builder().name("c1").build();
    DefaultImageMetadata metadata = new DefaultImageMetadata("img3", 789L, null);

    DefaultContainerUpdater updater = new DefaultContainerUpdater();
    DefaultContainerConfig updated =
        (DefaultContainerConfig) updater.updateDefaultImageMetadata(existing, metadata);

    assertEquals("img3", updated.getLastImageId());
    assertEquals(789L, updated.getImageSize());
    assertNull(updated.getInstallDate());
  }
}
