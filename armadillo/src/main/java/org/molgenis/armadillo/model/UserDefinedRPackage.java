package org.molgenis.armadillo.model;

public class UserDefinedRPackage {

  private String path;
  private String profile;

  public UserDefinedRPackage(String path, String profile) {
    this.path = path;
    this.profile = profile;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }
}
