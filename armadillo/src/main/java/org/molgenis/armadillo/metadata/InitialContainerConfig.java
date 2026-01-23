package org.molgenis.armadillo.metadata;

import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.InitialConfigBuilder;

public class InitialContainerConfig {
  private String name;
  private String image;
  private boolean autoUpdate;
  private UpdateSchedule updateSchedule;
  private String host;
  private int port;
  private Set<String> packageWhitelist;
  private Set<String> functionBlacklist;
  private Map<String, String> options;
  private String type;

  public ContainerConfig toContainerConfig(
      Map<String, InitialConfigBuilder> builderRegistry, String defaultType) {

    String configType = this.type != null ? this.type : defaultType;
    InitialConfigBuilder builder = builderRegistry.get(configType);

    if (builder == null) {
      throw new IllegalArgumentException("No container builder found for type: " + configType);
    }

    return builder.build(this);
  }

  public String getName() {
    return name;
  }

  public String getImage() {
    return image;
  }

  public boolean getAutoUpdate() {
    return autoUpdate;
  }

  public UpdateSchedule getUpdateSchedule() {
    return updateSchedule;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Set<String> getPackageWhitelist() {
    return packageWhitelist;
  }

  public Set<String> getFunctionBlacklist() {
    return functionBlacklist;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public String getType() {
    return type;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setAutoUpdate(Boolean autoUpdate) {
    this.autoUpdate = Boolean.TRUE.equals(autoUpdate);
  }

  public void setUpdateSchedule(UpdateSchedule updateSchedule) {
    this.updateSchedule = updateSchedule;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setPackageWhitelist(Set<String> packageWhitelist) {
    this.packageWhitelist = packageWhitelist;
  }

  public void setFunctionBlacklist(Set<String> functionBlacklist) {
    this.functionBlacklist = functionBlacklist;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  public void setType(String type) {
    this.type = type;
  }
}
