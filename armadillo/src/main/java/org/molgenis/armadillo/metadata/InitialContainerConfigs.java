package org.molgenis.armadillo.metadata;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "armadillo")
@Component
@Valid
public class InitialContainerConfigs {

  private List<InitialContainerConfig> containers;
  private String containerDefaultImage;
  private Set<String> datashieldDefaultWhitelist;

  public void setContainers(List<InitialContainerConfig> containers) {
    this.containers = containers;
  }

  public List<InitialContainerConfig> getContainers() {
    return containers;
  }

  public String getContainerDefaultImage() {
    return containerDefaultImage;
  }

  public void setContainerDefaultImage(String containerDefaultImage) {
    this.containerDefaultImage = containerDefaultImage;
  }

  public Set<String> getDatashieldDefaultWhitelist() {
    return datashieldDefaultWhitelist;
  }

  public void setDatashieldDefaultWhitelist(Set<String> datashieldDefaultWhitelist) {
    this.datashieldDefaultWhitelist = datashieldDefaultWhitelist;
  }
}
