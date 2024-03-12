package org.molgenis.armadillo.metadata;

import java.util.Map;
import java.util.Set;

/**
 * Profile that is passed as configuration parameters. Don't use at runtime.
 *
 * <p>This class can't be @AutoValue'd because Spring's @ConfigurationProperties can't bind to it
 * without setters.
 */
public class InitialProfileConfig {
  private boolean autoStart;
  private String name;
  private String image;
  private String host;
  private int port;
  private Set<String> packageWhitelist;
  private Set<String> functionBlacklist;
  private Map<String, String> options;

  public ProfileConfig toProfileConfig() {
    return ProfileConfig.create(
        autoStart, name, image, host, port, packageWhitelist, functionBlacklist, options);
  }

  public void setAutoStart(boolean autoStart) {
    this.autoStart = autoStart;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setImage(String image) {
    this.image = image;
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
}
