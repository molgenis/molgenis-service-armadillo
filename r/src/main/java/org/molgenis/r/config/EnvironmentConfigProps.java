package org.molgenis.r.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public class EnvironmentConfigProps {
  @NotEmpty private String name;
  @NotEmpty private String host = "localhost";
  private String image = "";
  @Positive private int port = 6311;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
