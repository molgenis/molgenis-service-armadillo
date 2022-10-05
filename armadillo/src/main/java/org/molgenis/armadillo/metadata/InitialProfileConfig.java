package org.molgenis.armadillo.metadata;

import java.util.Map;
import java.util.Set;

/**
 * This class can't be @AutoValue'd because Spring's @ConfigurationProperties can't bind to it
 * without setters.
 */
public class InitialProfileConfig {
  private String name;
  private String image;
  private String host;
  private int port;
  private Set<String> whitelist;
  private Map<String, String> options;

  public ProfileConfig toProfileConfig() {
    return ProfileConfig.create(name, null, host, port, whitelist, options);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public Set<String> getWhitelist() {
    return whitelist;
  }

  public void setWhitelist(Set<String> whitelist) {
    this.whitelist = whitelist;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }
}
