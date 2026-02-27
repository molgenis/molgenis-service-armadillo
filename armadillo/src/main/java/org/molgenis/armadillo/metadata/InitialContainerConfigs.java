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
  private ContainerSettings container = new ContainerSettings();

  public void setContainers(List<InitialContainerConfig> containers) {
    this.containers = containers;
  }

  public List<InitialContainerConfig> getContainers() {
    return containers;
  }

  public ContainerSettings getContainer() {
    return container;
  }

  public void setContainer(ContainerSettings container) {
    this.container = container;
  }

  public String getContainerDefaultImage() {
    return container.getDefaults().getImage();
  }

  public Set<String> getDatashieldDefaultWhitelist() {
    return container.getDefaults().getDatashield().getWhitelist();
  }

  public Set<String> getDatashieldDefaultBlacklist() {
    return container.getDefaults().getDatashield().getBlacklist();
  }

  public static class ContainerSettings {
    private Defaults defaults = new Defaults();

    public Defaults getDefaults() {
      return defaults;
    }

    public void setDefaults(Defaults defaults) {
      this.defaults = defaults;
    }
  }

  public static class Defaults {
    private String type = "ds";
    private String image = "datashield/molgenis-rock-base:latest";
    private DatashieldDefaults datashield = new DatashieldDefaults();

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getImage() {
      return image;
    }

    public void setImage(String image) {
      this.image = image;
    }

    public DatashieldDefaults getDatashield() {
      return datashield;
    }

    public void setDatashield(DatashieldDefaults datashield) {
      this.datashield = datashield;
    }
  }

  public static class DatashieldDefaults {
    private Set<String> whitelist = Set.of("dsBase", "dsTidyverse");
    private Set<String> blacklist = Set.of();

    public Set<String> getWhitelist() {
      return whitelist;
    }

    public void setWhitelist(Set<String> whitelist) {
      this.whitelist = whitelist;
    }

    public Set<String> getBlacklist() {
      return blacklist;
    }

    public void setBlacklist(Set<String> blacklist) {
      this.blacklist = blacklist;
    }
  }
}
