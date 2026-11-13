package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ContainersLoaderTest {

  @Test
  void createDefault_returnsEmptyMetadata() {
    ContainersLoader loader = new ContainersLoader();

    ContainersMetadata metadata = loader.createDefault();

    assertNotNull(metadata.getContainerMap());
    assertEquals(0, metadata.getContainerMap().size());
  }

  @Test
  void getTargetClass_returnsContainersMetadata() {
    ContainersLoader loader = new ContainersLoader();

    assertEquals(ContainersMetadata.class, loader.getTargetClass());
  }

  @Test
  void getJsonFilename_returnsContainersJson() {
    ContainersLoader loader = new ContainersLoader();

    assertEquals("containers.json", loader.getJsonFilename());
  }
}
