package org.molgenis.armadillo.metadata;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "armadillo")
@Component
@Valid
public class InitialContainerConfigs {

  private List<InitialContainerConfig> containers;

  public void setContainers(List<InitialContainerConfig> containers) {
    this.containers = containers;
  }

  public List<InitialContainerConfig> getContainers() {
    return containers;
  }
}
