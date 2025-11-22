package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonSerialize(as = ContainersStatusResponse.class)
public abstract class ContainersStatusResponse {
  @JsonProperty("image")
  public abstract String image();

  @JsonProperty("name")
  public abstract String name();

  @JsonProperty("config")
  public abstract String config();

  @JsonProperty("status")
  public abstract String status();

  public static ContainersStatusResponse create(
      String image, String name, String config, String status) {
    return new AutoValue_ContainersStatusResponse(image, name, config, status);
  }

  public static ContainersStatusResponse create(String image, String name) {
    return new AutoValue_ContainersStatusResponse(image, name, "[]", "DOCKER_MANAGEMENT_DISABLED");
  }
}
