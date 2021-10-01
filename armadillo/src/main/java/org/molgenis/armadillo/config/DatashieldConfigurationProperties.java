package org.molgenis.armadillo.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "datashield")
@Component
public class DatashieldConfigurationProperties {
  private final List<Profile> profiles;

  public DatashieldConfigurationProperties(
      List<Profile> profiles) {
    this.profiles = profiles;
  }

  public List<Profile> getProfiles() {
    return profiles;
  }
}
