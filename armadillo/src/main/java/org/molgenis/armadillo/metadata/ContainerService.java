package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.molgenis.armadillo.container.*;
import org.molgenis.armadillo.exceptions.DefaultContainerDeleteException;
import org.molgenis.armadillo.exceptions.InvalidFabWhitelistEntryException;
import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ContainerService {

  private static final Pattern FAB_HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
  private static final Pattern FAB_ID_PATTERN = Pattern.compile("^[\\w-]+/[\\w-]+$");

  private static final ObjectMapper FAB_WHITELIST_YAML_MAPPER =
      new ObjectMapper(new YAMLFactory())
          .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  private final ContainersLoader loader;
  private final InitialContainerConfigs initialContainer;
  private final ContainerScope containerScope;
  private final List<ContainerUpdater<? extends ContainerConfig>> updaters;
  private final DefaultContainerFactory defaultContainerFactory;
  private final Map<String, InitialConfigBuilder> initialConfigBuilders;
  private ContainersMetadata settings;

  public ContainerService(
      ContainersLoader containersLoader,
      InitialContainerConfigs initialContainerConfigs,
      ContainerScope containerScope,
      List<ContainerUpdater<? extends ContainerConfig>> allUpdaters,
      DefaultContainerFactory defaultContainerFactory,
      List<InitialConfigBuilder> allInitialConfigBuilders) {
    this.loader = requireNonNull(containersLoader);
    initialContainer = requireNonNull(initialContainerConfigs);
    this.containerScope = requireNonNull(containerScope);
    this.defaultContainerFactory = requireNonNull(defaultContainerFactory);

    this.initialConfigBuilders =
        allInitialConfigBuilders.stream()
            .collect(Collectors.toMap(InitialConfigBuilder::getType, builder -> builder));

    this.updaters = allUpdaters;
  }

  @jakarta.annotation.PostConstruct
  public void init() {
    runAsSystem(this::initialize);
  }

  public void initialize() {
    settings = loader.load();
    bootstrap();
  }

  public List<ContainerConfig> getAll() {
    return new ArrayList<>(settings.getContainers().values());
  }

  public ContainerConfig getByName(String containerName) {
    if (!settings.getContainers().containsKey(containerName)) {
      throw new UnknownContainerException(containerName);
    }
    return settings.getContainers().get(containerName);
  }

  public void upsert(ContainerConfig containerConfig) {

    String containerName = containerConfig.getName();

    if (containerConfig instanceof FlowerSupernodeContainerConfig supernode) {
      createPlaceholderFiles(supernode);
    } else if (containerConfig instanceof FlowerSuperexecContainerConfig superexec) {
      createPlaceholderFiles(superexec);
    }

    settings.getContainers().put(containerName, containerConfig);

    flushContainerBeans(containerName);
    save();
  }

  private void createPlaceholderFiles(FlowerSupernodeContainerConfig config) {
    createFileIfNotExists(config.getCaCertPath());
    createFileIfNotExists(config.getAuthPrivateKeyPath());
  }

  private void createPlaceholderFiles(FlowerSuperexecContainerConfig config) {
    createFileIfNotExists(config.getFabWhitelistPath());
  }

  private void createFileIfNotExists(String pathStr) {
    if (pathStr == null) return;
    Path path = Path.of(pathStr);
    if (Files.exists(path)) return;
    try {
      Files.createDirectories(path.getParent());
      Files.createFile(path);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create placeholder file: " + path, e);
    }
  }

  private void writeFabWhitelistFile(FlowerSuperexecContainerConfig config) {
    try {
      FAB_WHITELIST_YAML_MAPPER.writeValue(
          Path.of(config.getFabWhitelistPath()).toFile(), config.getFabWhitelist());
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to write FAB whitelist file: " + config.getFabWhitelistPath(), e);
    }
  }

  public void addFabWhitelistEntry(
      String containerName, String fabId, String fabVersion, String fabHash) {
    ContainerConfig existing = getByName(containerName);

    if (!(existing instanceof FlowerSuperexecContainerConfig superexec)) {
      throw new IllegalArgumentException(
          "Container '" + containerName + "' is not a Flower clientapp container");
    }

    validateFabHash(fabHash);
    validateFabId(fabId);
    validateFabVersion(fabVersion);

    List<WhitelistedApp> updatedWhitelist =
        superexec.getFabWhitelist().stream()
            .filter(
                entry ->
                    !(entry.fabId().equals(fabId)
                        && Objects.equals(entry.fabVersion(), fabVersion)))
            .collect(Collectors.toCollection(ArrayList::new));
    updatedWhitelist.add(new WhitelistedApp(fabId, fabVersion, fabHash));

    FlowerSuperexecContainerConfig updated =
        superexec.toBuilder().fabWhitelist(updatedWhitelist).build();
    upsert(updated);
    writeFabWhitelistFile(updated);
  }

  private void validateFabHash(String fabHash) {
    if (!FAB_HASH_PATTERN.matcher(fabHash).matches()) {
      throw new InvalidFabWhitelistEntryException(
          "fabHash must be a 64-character hexadecimal SHA-256 hash, got: " + fabHash);
    }
  }

  private void validateFabId(String fabId) {
    if (!FAB_ID_PATTERN.matcher(fabId).matches()) {
      throw new InvalidFabWhitelistEntryException(
          "fabId must look like 'publisher/name', got: " + fabId);
    }
  }

  private void validateFabVersion(String fabVersion) {
    if (fabVersion == null || fabVersion.isBlank()) {
      throw new InvalidFabWhitelistEntryException("fabVersion must not be blank");
    }
  }

  public java.util.Set<String> getPackageWhitelist(String containerName) {
    ContainerConfig config = getByName(containerName);

    if (config instanceof DatashieldContainerConfig dsConfig) {
      return dsConfig.getPackageWhitelist();
    }

    return java.util.Set.of();
  }

  public void addToWhitelist(String containerName, String pack) {
    ContainerConfig existing = getByName(containerName);

    if (!(existing instanceof DatashieldContainerConfig dsConfig)) {
      throw new UnsupportedOperationException(
          "Whitelisting is only supported for DataSHIELD containers. Found type: "
              + existing.getClass().getSimpleName());
    }

    Set<String> updatedWhitelist =
        new HashSet<>(
            dsConfig.getPackageWhitelist() == null ? Set.of() : dsConfig.getPackageWhitelist());
    updatedWhitelist.add(pack);

    ContainerConfig updatedConfig = dsConfig.toBuilder().packageWhitelist(updatedWhitelist).build();
    upsert(updatedConfig);
  }

  public void delete(String containerName) {
    if (containerName.equals(DEFAULT)) {
      throw new DefaultContainerDeleteException();
    }

    settings.getContainers().remove(containerName);
    flushContainerBeans(containerName);
    save();
  }

  private void flushContainerBeans(String containerName) {
    containerScope.removeAllContainerBeans(containerName);
  }

  private void save() {
    settings = loader.save(settings);
  }

  private void bootstrap() {
    if (settings == null) {
      return;
    }

    if (initialContainer.getContainers() != null) {
      initialContainer.getContainers().stream()
          .map(
              config ->
                  config.toContainerConfig(
                      initialConfigBuilders, defaultContainerFactory.getType()))
          .filter(container -> !settings.getContainers().containsKey(container.getName()))
          .forEach(this::upsert);
    }

    if (!settings.getContainers().containsKey(DEFAULT)) {
      upsert(defaultContainerFactory.createDefault());
    }
  }

  public void updateImageMetaData(
      String containerName,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {

    ContainerConfig existing = getByName(containerName);

    ContainerUpdater<? extends ContainerConfig> updater =
        updaters.stream()
            .filter(u -> u.supports(existing))
            .findFirst()
            .orElseThrow(
                () ->
                    new UnsupportedOperationException(
                        "No image metadata updater found for container type: "
                            + existing.getClass().getSimpleName()));

    DefaultImageMetadata defaultMetaData =
        new DefaultImageMetadata(newImageId, newImageSize, newInstallDate);

    ContainerConfig updated = updateDefaultMeta(updater, existing, defaultMetaData);

    if (updater instanceof OpenContainersUpdater<?> openUpdater) {
      OpenContainersImageMetadata openMetaData =
          new OpenContainersImageMetadata(newVersionId, newCreationDate);

      updated = updateOpenContainersMeta(openUpdater, updated, openMetaData);
    }

    settings.getContainers().put(containerName, updated);
    flushContainerBeans(containerName);
    save();
  }

  private static <T extends ContainerConfig> ContainerConfig updateDefaultMeta(
      ContainerUpdater<T> updater, ContainerConfig existing, DefaultImageMetadata metadata) {
    T typedConfig = updater.getSupportedType().cast(existing);
    return updater.updateDefaultImageMetadata(typedConfig, metadata);
  }

  private static <T extends ContainerConfig> ContainerConfig updateOpenContainersMeta(
      OpenContainersUpdater<T> updater,
      ContainerConfig existing,
      OpenContainersImageMetadata metadata) {
    T typedConfig = updater.getSupportedType().cast(existing);
    return updater.updateOpenContainersMetaData(typedConfig, metadata);
  }
}
