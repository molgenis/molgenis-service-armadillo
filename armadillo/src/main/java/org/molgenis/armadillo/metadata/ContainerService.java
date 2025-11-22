package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.container.ContainerScope;
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
  private ContainersMetadata settings;

  public ContainerService(
      ContainersLoader containersLoader,
      InitialContainerConfigs initialContainerConfigs,
      ContainerScope containerScope) {
    this.loader = requireNonNull(containersLoader);
    initialContainer = requireNonNull(initialContainerConfigs);
    this.containerScope = requireNonNull(containerScope);
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
        .put(
            containerName,
            ContainerConfig.create(
                containerName,
                containerConfig.getImage(),
                containerConfig.getAutoUpdate(),
                containerConfig.getUpdateSchedule(),
                containerConfig.getHost(),
                containerConfig.getPort(),
                containerConfig.getPackageWhitelist(),
                containerConfig.getFunctionBlacklist(),
                containerConfig.getOptions(),
                containerConfig.getLastImageId(),
                containerConfig.getVersionId(),
                containerConfig.getImageSize(),
                containerConfig.getCreationDate(),
                containerConfig.getInstallDate()));
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
      upsert(ContainerConfig.createDefault());
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

    ContainerConfig updated =
        ContainerConfig.create(
            existing.getName(),
            existing.getImage(),
            existing.getAutoUpdate(),
            existing.getUpdateSchedule(),
            existing.getHost(),
            existing.getPort(),
            existing.getPackageWhitelist(),
            existing.getFunctionBlacklist(),
            existing.getOptions(),
            newImageId,
            newVersionId,
            newImageSize,
            newCreationDate,
            newInstallDate != null ? newInstallDate : existing.getInstallDate());

    settings.getContainers().put(containerName, updated);
    flushContainerBeans(containerName);
    save();
  }
}
