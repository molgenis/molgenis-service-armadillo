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

    DatashieldContainerConfig defaultContainer =
        DatashieldContainerConfig.create(
            "default",
            "test",
            "localhost",
            6311,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            Set.of(),
            new HashMap<>(),
            List.of(),
            Map.of());

    containersMetadata.getContainers().put("default", defaultContainer);
    var containersLoader = new DummyContainersLoader(containersMetadata);

    var containerService =
        new ContainerService(
            containersLoader,
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());

    containerService.initialize();
    containerService.addToWhitelist("default", "dsOmics");

    verify(containerScope).removeAllContainerBeans("default");

    DatashieldContainerConfig updatedConfig =
        (DatashieldContainerConfig) containerService.getByName("default");
    assertTrue(updatedConfig.getPackageWhitelist().contains("dsOmics"));
  }

  @Test
  void testUpdateMetaDataDatashield() {
    String containerName = "default";
    String oldImageId = "sha256:old";
    String newImageId = "sha256:new";
    String newVersionId = "0.0.1";
    Long newImageSize = 123_456_789L;
    String newCreationDate = "2025-08-05T12:34:56Z";
    String newInstallDate = "2025-10-05T12:34:56Z";

    DatashieldContainerConfig existingContainer =
        DatashieldContainerConfig.create(
            containerName,
            "someImage",
            "localhost",
            6311,
            oldImageId,
            null,
            null,
            null,
            null,
            false,
            null,
            new HashSet<>(),
            new HashSet<>(),
            Map.of(),
            List.of(),
            Map.of());

    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put(containerName, existingContainer);

    ContainersLoader loader = mock(ContainersLoader.class);
    InitialContainerConfigs initialContainers = mock(InitialContainerConfigs.class);
    ContainerScope mockContainerScope = mock(ContainerScope.class);

    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var updater = new org.molgenis.armadillo.container.DatashieldContainerUpdater();

    ContainerService containerService =
        new ContainerService(
            loader,
            initialContainers,
            mockContainerScope,
            List.of(updater),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    containerService.updateImageMetaData(
        containerName, newImageId, newVersionId, newImageSize, newCreationDate, newInstallDate);

    ContainerConfig updated = containerService.getByName(containerName);
    assertEquals(newImageId, updated.getLastImageId());
    assertEquals(newImageSize, updated.getImageSize());
    assertEquals(newInstallDate, updated.getInstallDate());

    DatashieldContainerConfig dsUpdated = (DatashieldContainerConfig) updated;
    assertEquals(newVersionId, dsUpdated.getVersionId());
    assertEquals(newCreationDate, dsUpdated.getCreationDate());

    verify(mockContainerScope).removeAllContainerBeans(containerName);
    verify(loader).save(any());
  }
}
