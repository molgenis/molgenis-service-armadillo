package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.ContainerScope;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.DefaultContainerFactory;
import org.molgenis.armadillo.container.FlowerSuperexecContainerConfig;
import org.molgenis.armadillo.container.FlowerSupernodeContainerConfig;
import org.molgenis.armadillo.container.VanillaContainerConfig;
import org.molgenis.armadillo.container.VanillaContainerUpdater;
import org.molgenis.armadillo.container.WhitelistedApp;
import org.molgenis.armadillo.exceptions.DefaultContainerDeleteException;
import org.molgenis.armadillo.exceptions.InvalidFabWhitelistEntryException;
import org.molgenis.armadillo.exceptions.UnknownContainerException;

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
  void addToWhitelist_throwsWhenNotDatashield() {
    var containersMetadata = ContainersMetadata.create();

    VanillaContainerConfig defaultContainer =
        VanillaContainerConfig.create(
            "default",
            "image",
            "localhost",
            6311,
            null,
            null,
            null,
            List.of(),
            Map.of(),
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
            mock(DefaultContainerFactory.class),
            List.of());

    containerService.initialize();

    assertThrows(
        UnsupportedOperationException.class,
        () -> containerService.addToWhitelist("default", "dsOmics"));
  }

  @Test
  void delete_defaultContainerThrows() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

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

    assertThrows(DefaultContainerDeleteException.class, () -> containerService.delete("default"));
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

  @Test
  void updateImageMetaData_updatesDefaultContainer() {
    String containerName = "default";
    String newImageId = "sha256:new";
    Long newImageSize = 123_456_789L;
    String newInstallDate = "2025-10-05T12:34:56Z";

    VanillaContainerConfig existingContainer =
        VanillaContainerConfig.create(
            containerName,
            "someImage",
            "localhost",
            6311,
            null,
            null,
            null,
            List.of(),
            Map.of(),
            null,
            null,
            null,
            null);

    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put(containerName, existingContainer);

    ContainersLoader loader = mock(ContainersLoader.class);
    InitialContainerConfigs initialContainers = mock(InitialContainerConfigs.class);
    ContainerScope mockContainerScope = mock(ContainerScope.class);

    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var updater = new VanillaContainerUpdater();

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
        containerName, newImageId, "ignored", newImageSize, "ignored", newInstallDate);

    ContainerConfig updated = containerService.getByName(containerName);
    assertEquals(newImageId, updated.getLastImageId());
    assertEquals(newImageSize, updated.getImageSize());
    assertEquals(newInstallDate, updated.getInstallDate());

    verify(mockContainerScope).removeAllContainerBeans(containerName);
    verify(loader).save(any());
  }

  @Test
  void getPackageWhitelist_returnsEmptyForNonDatashield() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    assertEquals(Set.of(), containerService.getPackageWhitelist("default"));
  }

  @Test
  void getPackageWhitelist_returnsWhitelistForDatashield() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            DatashieldContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                new HashSet<>(Set.of("dsBase", "dsOmics")),
                Set.of(),
                Map.of(),
                List.of(),
                Map.of()));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    assertEquals(Set.of("dsBase", "dsOmics"), containerService.getPackageWhitelist("default"));
  }

  @Test
  void upsert_createsPlaceholderFilesForFlowerSupernode(@TempDir Path tempDir) {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    Path caCert = tempDir.resolve("ca.crt");
    Path credentials = tempDir.resolve("credentials");
    var config =
        FlowerSupernodeContainerConfig.builder()
            .name("flower-supernode")
            .image("flwr/supernode:1.32.1")
            .caCertPath(caCert.toString())
            .authPrivateKeyPath(credentials.toString())
            .build();

    containerService.upsert(config);

    assertTrue(Files.exists(caCert));
    assertTrue(Files.exists(credentials));
  }

  @Test
  void upsert_createsPlaceholderFileForFlowerSuperexec(@TempDir Path tempDir) {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    var config =
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .build();

    containerService.upsert(config);

    assertTrue(Files.exists(fabWhitelist));
  }

  private ContainerService containerServiceWithDefault(ContainersMetadata containersMetadata) {
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();
    return containerService;
  }

  @Test
  void upsert_writesFabWhitelistToYamlFile(@TempDir Path tempDir) throws java.io.IOException {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());

    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    var config =
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .fabWhitelist(List.of(new WhitelistedApp("@publisher/app", "1.0.0", "a".repeat(64))))
            .build();

    containerService.upsert(config);

    String content = Files.readString(fabWhitelist);
    assertTrue(content.contains("fab_id: \"@publisher/app\""));
    assertTrue(content.contains("fab_version: \"1.0.0\""));
    assertTrue(content.contains("fab_hash: \"" + "a".repeat(64) + "\""));
  }

  @Test
  void addFabWhitelistEntry_appendsNewEntry(@TempDir Path tempDir) {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());
    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    containerService.upsert(
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .build());

    containerService.addFabWhitelistEntry(
        "flower-clientapp-1", "@publisher/app", "1.0.0", "a".repeat(64));

    var updated = (FlowerSuperexecContainerConfig) containerService.getByName("flower-clientapp-1");
    assertEquals(
        List.of(new WhitelistedApp("@publisher/app", "1.0.0", "a".repeat(64))),
        updated.getFabWhitelist());
  }

  @Test
  void addFabWhitelistEntry_replacesExistingEntryForSameFabIdAndVersion(@TempDir Path tempDir) {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());
    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    containerService.upsert(
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .fabWhitelist(List.of(new WhitelistedApp("@publisher/app", "1.0.0", "a".repeat(64))))
            .build());

    containerService.addFabWhitelistEntry(
        "flower-clientapp-1", "@publisher/app", "1.0.0", "b".repeat(64));

    var updated = (FlowerSuperexecContainerConfig) containerService.getByName("flower-clientapp-1");
    assertEquals(
        List.of(new WhitelistedApp("@publisher/app", "1.0.0", "b".repeat(64))),
        updated.getFabWhitelist());
  }

  @Test
  void addFabWhitelistEntry_rejectsInvalidFabHash(@TempDir Path tempDir) {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());
    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    containerService.upsert(
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .build());

    assertThrows(
        InvalidFabWhitelistEntryException.class,
        () ->
            containerService.addFabWhitelistEntry(
                "flower-clientapp-1", "@publisher/app", "1.0.0", "not-a-hash"));
  }

  @Test
  void addFabWhitelistEntry_rejectsInvalidFabId(@TempDir Path tempDir) {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());
    Path fabWhitelist = tempDir.resolve("fab-whitelist.yaml");
    containerService.upsert(
        FlowerSuperexecContainerConfig.builder()
            .name("flower-clientapp-1")
            .image("flwr/superexec:1.32.1")
            .fabWhitelistPath(fabWhitelist.toString())
            .build());

    assertThrows(
        InvalidFabWhitelistEntryException.class,
        () ->
            containerService.addFabWhitelistEntry(
                "flower-clientapp-1", "not-an-app-id", "1.0.0", "a".repeat(64)));
  }

  @Test
  void addFabWhitelistEntry_rejectsNonSuperexecContainer() {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            containerService.addFabWhitelistEntry(
                "default", "@publisher/app", "1.0.0", "a".repeat(64)));
  }

  @Test
  void addFabWhitelistEntry_throwsForUnknownContainer() {
    var containerService = containerServiceWithDefault(ContainersMetadata.create());

    assertThrows(
        UnknownContainerException.class,
        () ->
            containerService.addFabWhitelistEntry(
                "does-not-exist", "@publisher/app", "1.0.0", "a".repeat(64)));
  }

  @Test
  void updateImageMetaData_throwsWhenNoUpdaterSupportsContainer() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(), // no updaters
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            containerService.updateImageMetaData(
                "default",
                "sha256:new",
                "v1",
                10L,
                "2025-01-01T00:00:00Z",
                "2025-01-02T00:00:00Z"));
  }

  @Test
  void delete_removesNonDefaultContainer() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "alpha",
            VanillaContainerConfig.create(
                "alpha",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    containerService.delete("alpha");

    assertThrows(UnknownContainerException.class, () -> containerService.getByName("alpha"));
    verify(containerScope).removeAllContainerBeans("alpha");
  }

  @Test
  void initialize_addsDefaultContainerWhenMissing() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "alpha",
            VanillaContainerConfig.create(
                "alpha",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var defaultContainerFactory = mock(DefaultContainerFactory.class);
    when(defaultContainerFactory.createDefault())
        .thenReturn(
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            defaultContainerFactory,
            List.of());
    containerService.initialize();

    assertNotNull(containerService.getByName("default"));
    verify(defaultContainerFactory).createDefault();
  }

  @Test
  void initialize_noopsWhenLoaderReturnsNull() {
    var loader = mock(ContainersLoader.class);
    when(loader.load()).thenReturn(null);

    var defaultContainerFactory = mock(DefaultContainerFactory.class);
    var containerService =
        new ContainerService(
            loader,
            initialContainerConfigs,
            containerScope,
            List.of(),
            defaultContainerFactory,
            List.of());

    assertDoesNotThrow(containerService::initialize);
    verify(defaultContainerFactory, never()).createDefault();
  }

  @Test
  void constructor_mapsInitialConfigBuilders() {
    var builder = mock(org.molgenis.armadillo.container.InitialConfigBuilder.class);
    when(builder.getType()).thenReturn("alpha");

    assertDoesNotThrow(
        () ->
            new ContainerService(
                new DummyContainersLoader(),
                initialContainerConfigs,
                containerScope,
                List.of(),
                mock(DefaultContainerFactory.class),
                List.of(builder)));
  }

  @Test
  void initialize_appliesInitialContainerConfigs() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var initialConfig = mock(InitialContainerConfig.class);
    when(initialConfig.toContainerConfig(any(), any()))
        .thenReturn(
            VanillaContainerConfig.create(
                "alpha",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));
    when(initialContainerConfigs.getContainers()).thenReturn(List.of(initialConfig));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    assertNotNull(containerService.getByName("alpha"));
    verify(initialConfig).toContainerConfig(any(), any());
  }

  @Test
  void initialize_skipsInitialContainerWhenAlreadyPresent() {
    var containersMetadata = ContainersMetadata.create();
    containersMetadata
        .getContainers()
        .put(
            "default",
            VanillaContainerConfig.create(
                "default",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));
    containersMetadata
        .getContainers()
        .put(
            "alpha",
            VanillaContainerConfig.create(
                "alpha",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));

    var initialConfig = mock(InitialContainerConfig.class);
    when(initialConfig.toContainerConfig(any(), any()))
        .thenReturn(
            VanillaContainerConfig.create(
                "alpha",
                "image",
                "localhost",
                6311,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                null));
    when(initialContainerConfigs.getContainers()).thenReturn(List.of(initialConfig));

    var containerService =
        new ContainerService(
            new DummyContainersLoader(containersMetadata),
            initialContainerConfigs,
            containerScope,
            List.of(),
            mock(DefaultContainerFactory.class),
            List.of());
    containerService.initialize();

    verify(containerScope, never()).removeAllContainerBeans("alpha");
  }
}
