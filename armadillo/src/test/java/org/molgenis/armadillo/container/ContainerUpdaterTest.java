package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.DefaultImageMetadata;

class ContainerUpdaterTest {

  @Test
  void supports_usesSupportedType() {
    ContainerUpdater<VanillaContainerConfig> updater =
        new ContainerUpdater<>() {
          @Override
          public Class<VanillaContainerConfig> getSupportedType() {
            return VanillaContainerConfig.class;
          }

          @Override
          public ContainerConfig updateDefaultImageMetadata(
              VanillaContainerConfig existingConfig, DefaultImageMetadata metadata) {
            return existingConfig;
          }
        };

    assertTrue(updater.supports(VanillaContainerConfig.createDefault()));
    assertFalse(
        updater.supports(
            DatashieldContainerConfig.builder()
                .name("ds")
                .packageWhitelist(java.util.Set.of())
                .functionBlacklist(java.util.Set.of())
                .build()));
  }
}
