package org.molgenis.armadillo.profile;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.molgenis.armadillo.DataShieldOptionsImpl;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.service.DataShieldProfileEnvironments;
import org.molgenis.r.RServeEnvironments;
import org.molgenis.r.service.PackageService;
import org.springframework.stereotype.Component;

@Component
public class Profiles {
  public static final String DEFAULT = "default";
  private final RServeEnvironments rServeEnvironments;
  private final PackageService packageService;
  private final Map<String, ProfileConfigProps> configs;
  private final Map<String, Profile> profilesByName;

  public Profiles(
      RServeEnvironments rServeEnvironments,
      PackageService packageService,
      DataShieldConfigProps datashieldProperties) {
    this.rServeEnvironments = rServeEnvironments;
    this.packageService = packageService;
    this.configs =
        datashieldProperties.getProfiles().stream()
            .collect(toUnmodifiableMap(ProfileConfigProps::getEnvironment, x -> x));
    this.profilesByName =
        datashieldProperties.getProfiles().stream()
            .map(ProfileConfigProps::getEnvironment)
            .map(profileName -> createProfile(packageService, profileName))
            .collect(toUnmodifiableMap(Profile::getProfileName, i -> i));
  }

  private Profile createProfile(PackageService packageService, String profileName) {
    var profileConfig = configs.get(profileName);
    var environmentName = profileConfig.getEnvironment();
    var rConnectionFactory = this.rServeEnvironments.getConnectionFactory(environmentName);
    var profileEnvironments =
        new DataShieldProfileEnvironments(
            profileName, packageService, rConnectionFactory, profileConfig);
    var dataShieldOptions =
        new DataShieldOptionsImpl(profileConfig, this.packageService, rConnectionFactory);
    return new Profile(profileConfig, rConnectionFactory, profileEnvironments, dataShieldOptions);
  }

  @PostConstruct
  public void init() {
    profilesByName.values().forEach(Profile::init);
  }

  public Collection<Profile> getProfiles() {
    return profilesByName.values();
  }

  public Profile getDefaultProfile() {
    return getProfileByName(DEFAULT).orElseThrow();
  }

  public Optional<Profile> getProfileByName(String profileName) {
    return Optional.ofNullable(profilesByName.get(profileName));
  }
}
