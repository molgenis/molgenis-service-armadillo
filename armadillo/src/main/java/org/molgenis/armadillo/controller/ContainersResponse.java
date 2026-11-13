package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonSerialize(as = ContainersResponse.class)
public abstract class ContainersResponse {
  @JsonProperty("available")
  public abstract List<String> available();

  @JsonProperty("current")
  @Nullable
  public abstract String current();

  public static ContainersResponse create(List<String> available, String current) {
    return new AutoValue_ContainersResponse(available, current);
  }
}
