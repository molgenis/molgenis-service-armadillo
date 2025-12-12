package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.armadillo.container.*;
import org.molgenis.armadillo.container.ContainerWhitelister;
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
  private final Map<Class<? extends ContainerConfig>, ContainerUpdater> updaters;
  private final Map<Class<? extends ContainerConfig>, ContainerWhitelister> whitelisters;
  private final DefaultContainerFactory defaultContainerFactory;
  private ContainersMetadata settings;

  public ContainerService(
      ContainersLoader containersLoader,
      InitialContainerConfigs initialContainerConfigs,
      ContainerScope containerScope,
      List<ContainerUpdater> allUpdaters,
      List<ContainerWhitelister> allWhitelisters,
      DefaultContainerFactory defaultContainerFactory) {
    this.loader = requireNonNull(containersLoader);
    initialContainer = requireNonNull(initialContainerConfigs);
    this.containerScope = requireNonNull(containerScope);
    this.defaultContainerFactory = requireNonNull(defaultContainerFactory);

    this.updaters =
        allUpdaters.stream()
            .collect(Collectors.toMap(ContainerUpdater::supportsConfigType, updater -> updater));

    runAsSystem(this::initialize);

    this.whitelisters = initializeWhitelisters(allWhitelisters);
  }

  private Map<Class<? extends ContainerConfig>, ContainerWhitelister> initializeWhitelisters(
      List<ContainerWhitelister> allWhitelisters) {

    Map<Class<? extends ContainerConfig>, ContainerWhitelister> map = new HashMap<>();
    ContainerWhitelister nullWhitelister = null;

    for (ContainerWhitelister whitelister : allWhitelisters) {

      map.put(whitelister.supportsConfigType(), whitelister);

      if (whitelister instanceof NullContainerWhitelister) {
        nullWhitelister = whitelister;
      }
    }

    if (nullWhitelister != null) {
      map.put(DefaultContainerConfig.class, nullWhitelister);
    }

    return map;
  }

  /**
   * Initialization separated from constructor so that it can be called in WebMvc tests
   * <strong>after</strong> mocks have been initialized.
   */
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

    ContainerWhitelister whitelister =
        whitelisters.getOrDefault(
            existing.getClass(), whitelisters.get(DefaultContainerConfig.class));

    if (whitelister instanceof NullContainerWhitelister) {
      throw new UnsupportedOperationException(
          "Whitelisting is only supported for DataSHIELD containers (and types with a dedicated whitelister). Found type: "
              + existing.getClass().getSimpleName());
    }

    whitelister.addToWhitelist(existing, pack);
    flushContainerBeans(containerName);
    save();
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
          .map(InitialContainerConfig::toContainerConfig)
          .filter(container -> !settings.getContainers().containsKey(container.getName()))
          .forEach(this::upsert);
    }

    if (!settings.getContainers().containsKey(DEFAULT)) {
      upsert(defaultContainerFactory.createDefault());
    }
  }

  // In ContainerService.java

  public void updateImageMetaData(
      String containerName,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {

    ContainerConfig existing = getByName(containerName);

    ContainerUpdater updater = updaters.get(existing.getClass());

    if (updater == null) {
      throw new UnsupportedOperationException(
          "No image metadata updater found for container type: "
              + existing.getClass().getSimpleName());
    }

    ContainerConfig updated =
        updater.updateImageMetaData(
            existing, newImageId, newVersionId, newImageSize, newCreationDate, newInstallDate);

    // 3. Storage remains generic
    settings.getContainers().put(containerName, updated);
    flushContainerBeans(containerName);
    save();
  }
}
