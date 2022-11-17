package org.molgenis.armadillo.metadata;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Collects the profiles that are passed as configuration parameters. */
@ConfigurationProperties(prefix = "armadillo")
@Component
@Validated
public class InitialProfileConfigs {

  private List<InitialProfileConfig> profiles;

  public void setProfiles(List<InitialProfileConfig> profiles) {
    this.profiles = profiles;
  }

  public List<InitialProfileConfig> getProfiles() {
    return profiles;
  }
}
