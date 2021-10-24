package org.molgenis.r.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "rserve")
@Component
public class RServeConfig {
  private final List<EnvironmentConfigProps> environmentConfigProps;

  public RServeConfig(List<EnvironmentConfigProps> environmentConfigProps) {
    this.environmentConfigProps = environmentConfigProps;
  }

  public List<EnvironmentConfigProps> getEnvironments() {
    return environmentConfigProps;
  }
}
