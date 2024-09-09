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
  @PreAuthorize("hasRole('ROLE_SU')")
  public void initialize() {
    settings = loader.load();
    bootstrap();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public List<ProfileConfig> getAll() {
    return new ArrayList<>(settings.getProfiles().values());
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public ProfileConfig getByName(String profileName) {
    if (!settings.getProfiles().containsKey(profileName)) {
      throw new UnknownProfileException(profileName);
    }
    return settings.getProfiles().get(profileName);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void upsert(ProfileConfig profileConfig) {
    String profileName = profileConfig.getName();
    settings
        .getProfiles()
        .put(
            profileName,
            ProfileConfig.create(
                profileName,
                profileConfig.getImage(),
                profileConfig.getHost(),
                profileConfig.getPort(),
                profileConfig.getPackageWhitelist(),
                profileConfig.getFunctionBlacklist(),
                profileConfig.getOptions()));

    flushProfileBeans(profileName);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void addToWhitelist(String profileName, String pack) {
    getByName(profileName).getPackageWhitelist().add(pack);
    flushProfileBeans(profileName);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void delete(String profileName) {
    if (profileName.equals(DEFAULT)) {
      throw new DefaultProfileDeleteException();
    }

    settings.getProfiles().remove(profileName);
    flushProfileBeans(profileName);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  private void flushProfileBeans(String profileName) {
    profileScope.removeAllProfileBeans(profileName);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  private void save() {
    settings = loader.save(settings);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
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
}
