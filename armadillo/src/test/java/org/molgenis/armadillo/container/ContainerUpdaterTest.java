package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.DefaultImageMetadata;

class ContainerUpdaterTest {

  @Test
  void supports_usesSupportedType() {
    ContainerUpdater<DefaultContainerConfig> updater =
        new ContainerUpdater<>() {
          @Override
          public Class<DefaultContainerConfig> getSupportedType() {
            return DefaultContainerConfig.class;
          }

          @Override
          public ContainerConfig updateDefaultImageMetadata(
              DefaultContainerConfig existingConfig, DefaultImageMetadata metadata) {
            return existingConfig;
          }
        };

    assertTrue(updater.supports(DefaultContainerConfig.createDefault()));
    assertFalse(
        updater.supports(
            DatashieldContainerConfig.builder()
                .name("ds")
                .packageWhitelist(java.util.Set.of())
                .functionBlacklist(java.util.Set.of())
                .build()));
  }
}
