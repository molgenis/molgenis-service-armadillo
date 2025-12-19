package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.ContainerScope;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.DefaultContainerFactory;

@ExtendWith(MockitoExtension.class)
class ContainerServiceTest {

  @Mock private ContainerScope containerScope;

  @Mock private InitialContainerConfigs initialContainerConfigs;

  @Test
  void addToWhitelist() {
    var containersMetadata = ContainersMetadata.create();
    ContainerConfig defaultContainer =
        DatashieldContainerConfig.create(
            "default",
            "test",
            false,
            null,
            "localhost",
            1234,
            new HashSet<>(),
            Set.of(),
            new HashMap<>(),
            null,
            null,
            null,
            null,
            null);
    containersMetadata.getContainers().put("default", defaultContainer);
    var containersLoader = new DummyContainersLoader(containersMetadata);
    var containerService =
        new ContainerService(
            containersLoader,
            initialContainerConfigs,
            containerScope,
            List.of(),
            List.of(new org.molgenis.armadillo.container.DatashieldContainerWhitelister()),
            mock(DefaultContainerFactory.class),
            List.of());

    containerService.initialize();

    containerService.addToWhitelist("default", "dsOmics");

    verify(containerScope).removeAllContainerBeans("default");
    assertTrue(
        ((DatashieldContainerConfig) containerService.getByName("default"))
            .getPackageWhitelist()
            .contains("dsOmics"));
  }

  @Test
  void testUpdateMetaData() {
    String containerName = "default";
    String oldImageId = "sha256:old";
    String newImageId = "sha256:new";
    String newVersionId = "0.0.1";
    Long newImageSize = 123_456_789L;
    String newCreationDate = "2025-08-05T12:34:56Z";
    String newInstallDate = "2025-10-05T12:34:56Z";

    // Create an existing container config with oldImageId
    ContainerConfig existingContainer =
        DatashieldContainerConfig.create(
            containerName,
            "someImage",
            false,
            null,
            "localhost",
            6311,
            new HashSet<>(),
            new HashSet<>(),
            Map.of(),
            oldImageId,
            null,
            null,
            null,
            null);

    // Setup ContainersMetadata and add existing container
    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put(containerName, existingContainer);

    // Mock loader and dependencies
    ContainersLoader loader = mock(ContainersLoader.class);
    InitialContainerConfigs initialContainers = mock(InitialContainerConfigs.class);
    ContainerScope mockContainerScope = mock(ContainerScope.class);

    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ContainerService containerService =
        new ContainerService(
            loader,
            initialContainers,
            mockContainerScope,
            List.of(),
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    // Act: update the image id, version, and size
    containerService.updateImageMetaData(
        containerName, newImageId, newVersionId, newImageSize, newCreationDate, newInstallDate);

    // Assert that the container has been updated
    ContainerConfig updated = containerService.getByName(containerName);
    assertEquals(newImageId, updated.getLastImageId());
    assertEquals(newImageSize, updated.getImageSize());

    // Verify flush and save were called
    verify(mockContainerScope).removeAllContainerBeans(containerName);
    verify(loader).save(any());
  }
}
