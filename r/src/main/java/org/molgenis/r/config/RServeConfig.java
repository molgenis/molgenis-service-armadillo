package org.molgenis.r.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "rserve")
@Component
public class RServeConfig {
  private final List<Environment> environments;

  public RServeConfig(List<Environment> environments) {
    this.environments = environments;
  }

  public List<Environment> getEnvironments() {
    return environments;
  }
}
