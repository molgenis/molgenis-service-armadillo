package org.molgenis.armadillo.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;

@Validated
public class Profile {
  private Map<String, String> options = new HashMap<>();
  @NotEmpty private Set<String> whitelist = new HashSet<>();
  @NotEmpty private String server;

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  public Set<String> getWhitelist() {
    return whitelist;
  }

  public void setWhitelist(Set<String> whitelist) {
    this.whitelist = whitelist;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  @Override
  public String toString() {
    return "Profile{" +
        "server='" + server + '\'' +
        '}';
  }
}
