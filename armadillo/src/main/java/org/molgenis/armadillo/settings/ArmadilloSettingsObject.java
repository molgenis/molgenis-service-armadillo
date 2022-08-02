package org.molgenis.armadillo.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArmadilloSettingsObject {
  private Map<String, User> users = new LinkedHashMap<>();

  public ArmadilloSettingsObject() {}

  public Map<String, User> getUsers() {
    return users;
  }

  public void setUsers(Map<String, User> users) {
    this.users = users;
  }
}
