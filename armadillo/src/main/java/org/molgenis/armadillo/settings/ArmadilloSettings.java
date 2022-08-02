package org.molgenis.armadillo.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArmadilloSettings {
  private Map<String, UserDetails> users = new LinkedHashMap<>();

  public Map<String, UserDetails> getUsers() {
    return users;
  }
}
