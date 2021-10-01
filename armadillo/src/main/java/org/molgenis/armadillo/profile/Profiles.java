package org.molgenis.armadillo.profile;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.molgenis.armadillo.DataShieldOptionsImpl;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.service.DataShieldProfileEnvironments;
import org.molgenis.r.RServers;
import org.molgenis.r.service.PackageService;
import org.springframework.stereotype.Component;

@Component
public class Profiles {
  private final RServers rServers;
  private final PackageService packageService;
  private final Map<String, ProfileConfigProps> configs;
  private final Map<String, Profile> profilesByName;

  public Profiles(
      RServers rServers,
      PackageService packageService,
      DataShieldConfigProps datashieldProperties) {
    this.rServers = rServers;
    this.packageService = packageService;
    this.configs =
        datashieldProperties.getProfiles().stream()
            .collect(toUnmodifiableMap(ProfileConfigProps::getNode, x -> x));
    this.profilesByName =
        datashieldProperties.getProfiles().stream()
            .map(ProfileConfigProps::getNode)
            .map(profileName -> createProfile(packageService, profileName))
            .collect(toUnmodifiableMap(Profile::getProfileName, i -> i));
  }

  private Profile createProfile(PackageService packageService, String profileName) {
    var profileConfig = configs.get(profileName);
    var rConnectionFactory = this.rServers.getConnectionFactory(profileName);
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

  public Profile getProfile(String profileName) {
    return profilesByName.get(profileName);
  }
}
