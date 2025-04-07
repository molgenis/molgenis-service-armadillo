package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonSerialize(as = ProfilesStatusResponse.class)
public abstract class ProfilesStatusResponse {
  @JsonProperty("image")
  public abstract String image();

  @JsonProperty("name")
  public abstract String name();

  @JsonProperty("config")
  public abstract String config();

  @JsonProperty("status")
  public abstract String status();

  public static ProfilesStatusResponse create(
      String image, String name, String config, String status) {
    return new AutoValue_ProfilesStatusResponse(image, name, config, status);
  }

  public static ProfilesStatusResponse create(String image, String name) {
    return new AutoValue_ProfilesStatusResponse(image, name, "[]", "DOCKER_MANAGEMENT_DISABLED");
  }
}
