package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private final Map<Class<? extends AbstractContainerConfig>, ContainerUpdater> updaters;
  private ContainersMetadata settings;

  public ContainerService(
      ContainersLoader containersLoader,
      InitialContainerConfigs initialContainerConfigs,
      ContainerScope containerScope,
      List<ContainerUpdater> allUpdaters) { // <-- NEW PARAMETER
    this.loader = requireNonNull(containersLoader);
    initialContainer = requireNonNull(initialContainerConfigs);
    this.containerScope = requireNonNull(containerScope);

    this.updaters =
        allUpdaters.stream()
            .collect(
                Collectors.toMap(
                    updater -> {
                      // We use instanceof checks to map the updater implementation to its Config
                      // class
                      if (updater instanceof DatashieldContainerUpdater) {
                        return DatashieldContainerConfig.class;
                      }
                      if (updater instanceof DefaultContainerUpdater) {
                        return DefaultContainerConfig.class;
                      }
                      // Handle future container types here
                      throw new IllegalStateException(
                          "Unknown ContainerUpdater implementation: "
                              + updater.getClass().getName());
                    },
                    updater -> updater));

    runAsSystem(this::initialize);
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

    settings
        .getContainers()
        .put(containerName, containerConfig); // Stores any subclass instance directly

    flushContainerBeans(containerName);
    save();
  }

  public void addToWhitelist(String containerName, String pack) {
    getByName(containerName).getPackageWhitelist().add(pack);
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
      upsert(DatashieldContainerConfig.createDefault());
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
