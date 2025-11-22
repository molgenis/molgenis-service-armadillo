package org.molgenis.armadillo.metadata;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Collects the profiles that are passed as configuration parameters. */
@ConfigurationProperties(prefix = "armadillo")
@Component
@Valid
public class InitialContainerConfigs {

  private List<InitialContainerConfig> profiles;

  public void setProfiles(List<InitialContainerConfig> profiles) {
    this.profiles = profiles;
  }

  public List<InitialContainerConfig> getProfiles() {
    return profiles;
  }
}
