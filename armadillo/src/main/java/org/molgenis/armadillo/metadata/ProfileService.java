package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ProfileService {

  private final ProfilesLoader loader;
  private final InitialProfileConfigs initialProfiles;
  private ProfilesMetadata settings;

  public ProfileService(
      ProfilesLoader profilesLoader, InitialProfileConfigs initialProfileConfigs) {
    this.loader = requireNonNull(profilesLoader);
    initialProfiles = requireNonNull(initialProfileConfigs);
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
                profileConfig.getHost(),
                profileConfig.getPort(),
                profileConfig.getWhitelist(),
                profileConfig.getOptions()));
    save();
  }

  public void delete(String profileName) {
    settings.getProfiles().remove(profileName);
    save();
  }

  private void save() {
    settings = loader.save(settings);
  }

  private void bootstrap() {
    initialProfiles.getProfiles().stream()
        .map(InitialProfileConfig::toProfileConfig)
        .filter(profile -> !settings.getProfiles().containsKey(profile.getName()))
        .forEach(this::upsert);
  }
}
