package org.molgenis.armadillo.metadata;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Collects the profiles that are passed as configuration parameters. */
@ConfigurationProperties(prefix = "datashield")
@Component
@Validated
public class InitialProfileConfigs {

  @NotEmpty @Valid private List<InitialProfileConfig> profiles;

  public void setProfiles(List<InitialProfileConfig> profiles) {
    this.profiles = profiles;
  }

  public List<InitialProfileConfig> getProfiles() {
    return profiles;
  }
}
