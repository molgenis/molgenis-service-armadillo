package org.molgenis.r.rock;

public class RockApplication {

  private final String url;

  private final String user;

  private final String password;

  public RockApplication(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}
