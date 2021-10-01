package org.molgenis.armadillo;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import org.molgenis.armadillo.config.DatashieldConfigurationProperties;
import org.molgenis.armadillo.config.Profile;
import org.molgenis.armadillo.service.ArmadilloConnectionFactoryImpl;
import org.molgenis.r.RServers;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.springframework.stereotype.Component;

@Component
public class ArmadilloSessionFactory {
  private final RServers rServers;
  private final PackageService packageService;
  private final ProcessService processService;
  private final Map<String, Profile> profiles;

  public ArmadilloSessionFactory(RServers rServers,
      PackageService packageService, ProcessService processService,
      DatashieldConfigurationProperties datashieldProperties) {
    this.rServers = rServers;
    this.packageService = packageService;
    this.processService = processService;
    this.profiles = datashieldProperties.getProfiles().stream()
        .collect(toUnmodifiableMap(Profile::getServer, x -> x));
  }

  public ArmadilloSession createSession(String profileName) {
    var connectionFactory = rServers.getConnectionFactory(profileName);
    var profile = profiles.get(profileName);
    DataShieldOptions options =
        new DataShieldOptionsImpl(profile, packageService, connectionFactory);
    return new ArmadilloSession(
            new ArmadilloConnectionFactoryImpl(options, connectionFactory), processService);
  }
}
