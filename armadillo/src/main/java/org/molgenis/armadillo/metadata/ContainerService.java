package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.exceptions.DefaultContainerDeleteException;
import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.molgenis.armadillo.profile.ProfileScope;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ContainerService {

  private final ContainersLoader loader;
  private final InitialContainerConfigs initialProfiles;
  private final ProfileScope profileScope;
  private ContainersMetadata settings;

  public ContainerService(
      ContainersLoader containersLoader,
      InitialContainerConfigs initialContainerConfigs,
      ProfileScope profileScope) {
    this.loader = requireNonNull(containersLoader);
    initialProfiles = requireNonNull(initialContainerConfigs);
    this.profileScope = requireNonNull(profileScope);
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
    return new ArrayList<>(settings.getProfiles().values());
  }

  public ContainerConfig getByName(String profileName) {
    if (!settings.getProfiles().containsKey(profileName)) {
      throw new UnknownContainerException(profileName);
    }
    return settings.getProfiles().get(profileName);
  }

  public void upsert(ContainerConfig containerConfig) {
    String profileName = containerConfig.getName();
    settings
        .getProfiles()
        .put(
            profileName,
            ContainerConfig.create(
                profileName,
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
    flushProfileBeans(profileName);
    save();
  }

  public void addToWhitelist(String profileName, String pack) {
    getByName(profileName).getPackageWhitelist().add(pack);
    flushProfileBeans(profileName);
    save();
  }

  public void delete(String profileName) {
    if (profileName.equals(DEFAULT)) {
      throw new DefaultContainerDeleteException();
    }

    settings.getProfiles().remove(profileName);
    flushProfileBeans(profileName);
    save();
  }

  private void flushProfileBeans(String profileName) {
    profileScope.removeAllProfileBeans(profileName);
  }

  private void save() {
    settings = loader.save(settings);
  }

  private void bootstrap() {
    if (settings == null) {
      return;
    }

    if (initialProfiles.getProfiles() != null) {
      initialProfiles.getProfiles().stream()
          .map(InitialContainerConfig::toProfileConfig)
          .filter(profile -> !settings.getProfiles().containsKey(profile.getName()))
          .forEach(this::upsert);
    }

    if (!settings.getProfiles().containsKey(DEFAULT)) {
      upsert(ContainerConfig.createDefault());
    }
  }

  public void updateImageMetaData(
      String profileName,
      String newImageId,
      String newVersionId,
      Long newImageSize,
      String newCreationDate,
      @Nullable String newInstallDate) {
    ContainerConfig existing = getByName(profileName);

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

    settings.getProfiles().put(profileName, updated);
    flushProfileBeans(profileName);
    save();
  }
}
