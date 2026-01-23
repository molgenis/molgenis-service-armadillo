package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.armadillo.container.*;
import org.molgenis.armadillo.exceptions.DefaultContainerDeleteException;
import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ContainerService {

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

    settings.getContainers().put(containerName, containerConfig);

    flushContainerBeans(containerName);
    save();
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
          .map(config -> config.toContainerConfig(initialConfigBuilders))
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

    DefaultImageMetaData defaultMetaData =
        new DefaultImageMetaData(newImageId, newImageSize, newInstallDate);

    ContainerConfig updated = updateDefaultMeta(updater, existing, defaultMetaData);

    if (updater instanceof OpenContainersUpdater<?> openUpdater) {
      OpenContainersImageMetaData openMetaData =
          new OpenContainersImageMetaData(newVersionId, newCreationDate);

      updated = updateOpenContainersMeta(openUpdater, updated, openMetaData);
    }

    settings.getContainers().put(containerName, updated);
    flushContainerBeans(containerName);
    save();
  }

  private static <T extends ContainerConfig> ContainerConfig updateDefaultMeta(
      ContainerUpdater<T> updater, ContainerConfig existing, DefaultImageMetaData metadata) {
    T typedConfig = updater.getSupportedType().cast(existing);
    return updater.updateDefaultImageMetaData(typedConfig, metadata);
  }

  private static <T extends ContainerConfig> ContainerConfig updateOpenContainersMeta(
      OpenContainersUpdater<T> updater,
      ContainerConfig existing,
      OpenContainersImageMetaData metadata) {
    T typedConfig = updater.getSupportedType().cast(existing);
    return updater.updateOpenContainersMetaData(typedConfig, metadata);
  }
}
