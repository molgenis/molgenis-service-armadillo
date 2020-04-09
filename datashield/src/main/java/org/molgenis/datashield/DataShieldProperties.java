package org.molgenis.datashield;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "datashield")
@Component
public class DataShieldProperties {

  private Map<String, String> options = new HashMap<>();
  private Set<String> whitelist = new HashSet<>();

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
}
