package org.molgenis.armadillo.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.springframework.validation.annotation.Validated;

@Validated
public class ProfileConfigProps extends EnvironmentConfigProps {

  private Map<String, String> options = new HashMap<>();
  @NotEmpty private final Set<String> whitelist = new HashSet<>();
  private String dockerImage;

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

  @Override
  public String toString() {
    return "ProfileConfigProps{" + "name='" + getName() + "'}";
  }

  public String getDockerImage() {
    return dockerImage;
  }

  public void setDockerImage(String dockerImage) {
    this.dockerImage = dockerImage;
  }
}
