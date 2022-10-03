package org.molgenis.armadillo.metadata;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.molgenis.armadillo.profile.DockerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ProfileService {

  private final ProfilesLoader loader;
  private final DockerService dockerService;
  private ProfilesMetadata settings;

  public ProfileService(DockerService dockerService, ProfilesLoader profilesLoader) {
    this.loader = requireNonNull(profilesLoader);
    this.dockerService = dockerService;
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
    // TODO profile status isn't included here
    return new ArrayList<>(settings.getProfiles().values());
  }

  public ProfileConfig getByName(String profileName) {
    if (!settings.getProfiles().containsKey(profileName)) {
      throw new UnknownProfileException(profileName);
    }
    ProfileConfig config = settings.getProfiles().get(profileName);
    // add the status
    return ProfileConfig.create(
        config.getName(),
        config.getImage(),
        config.getHost(),
        config.getPort(),
        config.getWhitelist(),
        config.getOptions(),
        dockerService != null ? dockerService.getProfileStatus(config) : null);
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
                profileConfig.getOptions(),
                null));
    save();
  }

  public void delete(String profileName) {
    settings.getProfiles().remove(profileName);
    save();
  }

  public void start(String profileName) {
    if (dockerService == null) {
      throw new IllegalStateException("Docker management disabled but attempting to start image");
    }

    var profile = getByName(profileName);
    dockerService.startProfile(profile);
  }

  public void stop(String profileName) {
    if (dockerService == null) {
      throw new IllegalStateException("Docker management disabled but attempting to stop image");
    }

    dockerService.removeProfile(profileName);
  }

  private void save() {
    settings = loader.save(settings);
  }

  private void bootstrap() {}
}
