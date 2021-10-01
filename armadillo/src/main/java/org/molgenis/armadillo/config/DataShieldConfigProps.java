package org.molgenis.armadillo.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "datashield")
@Component
public class DataShieldConfigProps {
  private final List<ProfileConfigProps> profiles;

  public DataShieldConfigProps(
      List<ProfileConfigProps> profiles) {
    this.profiles = profiles;
  }

  public List<ProfileConfigProps> getProfiles() {
    return profiles;
  }
}
