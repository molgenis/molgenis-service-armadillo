package org.molgenis.r.config;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;

public class EnvironmentConfigProps {
  @NotEmpty private String name;
  @NotEmpty private String host = "localhost";
  @Positive private int port = 6311;

  private String username;

  private String password;

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

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
