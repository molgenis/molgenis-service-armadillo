package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.exceptions.DefaultProfileDeleteException;
import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.molgenis.armadillo.profile.ProfileScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ProfileService {

  private final ProfilesLoader loader;
  private final InitialProfileConfigs initialProfiles;
  private final ProfileScope profileScope;
  private ProfilesMetadata settings;

  public ProfileService(
      ProfilesLoader profilesLoader,
      InitialProfileConfigs initialProfileConfigs,
      ProfileScope profileScope) {
    this.loader = requireNonNull(profilesLoader);
    initialProfiles = requireNonNull(initialProfileConfigs);
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

  public List<ProfileConfig> getAll() {
    return new ArrayList<>(settings.getProfiles().values());
  }

  public ProfileConfig getByName(String profileName) {
    if (!settings.getProfiles().containsKey(profileName)) {
      throw new UnknownProfileException(profileName);
    }
    return settings.getProfiles().get(profileName);
  }

  public void upsert(ProfileConfig profileConfig) {
    String profileName = profileConfig.getName();
    settings
        .getProfiles()
        .put(
            profileName,
            ProfileConfig.create(
                profileName,
                profileConfig.getImage(),
                profileConfig.getAutoUpdate(),
                profileConfig.getAutoUpdateSchedule(),
                profileConfig.getHost(),
                profileConfig.getPort(),
                profileConfig.getPackageWhitelist(),
                profileConfig.getFunctionBlacklist(),
                profileConfig.getOptions(),
                profileConfig.getLastImageId(),
                profileConfig.getVersionId(),
                profileConfig.getImageSize()));
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
      throw new DefaultProfileDeleteException();
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
          .map(InitialProfileConfig::toProfileConfig)
          .filter(profile -> !settings.getProfiles().containsKey(profile.getName()))
          .forEach(this::upsert);
    }

    if (!settings.getProfiles().containsKey(DEFAULT)) {
      upsert(ProfileConfig.createDefault());
    }
  }

  public void updateImageMetaData(
      String profileName, String newImageId, String newVersionId, Long newImageSize) {
    ProfileConfig existing = getByName(profileName);

    ProfileConfig updated =
        ProfileConfig.create(
            existing.getName(),
            existing.getImage(),
            existing.getAutoUpdate(),
            existing.getAutoUpdateSchedule(),
            existing.getHost(),
            existing.getPort(),
            existing.getPackageWhitelist(),
            existing.getFunctionBlacklist(),
            existing.getOptions(),
            newImageId,
            newVersionId,
            newImageSize);

    settings.getProfiles().put(profileName, updated);
    flushProfileBeans(profileName);
    save();
  }
}
