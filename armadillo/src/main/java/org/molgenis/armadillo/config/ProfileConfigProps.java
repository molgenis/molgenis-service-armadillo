package org.molgenis.armadillo.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;

@Validated
public class ProfileConfigProps {

  private Map<String, String> options = new HashMap<>();
  @NotEmpty private final Set<String> whitelist = new HashSet<>();
  @NotEmpty private String name;
  @NotEmpty private String environment;

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  public Set<String> getWhitelist() {
    return whitelist;
  }

  public void addToWhitelist(String pkg) {
    whitelist.add(pkg);
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "ProfileConfigProps{"
        + "name='"
        + name
        + '\''
        + ", environment='"
        + environment
        + '\''
        + '}';
  }
}
