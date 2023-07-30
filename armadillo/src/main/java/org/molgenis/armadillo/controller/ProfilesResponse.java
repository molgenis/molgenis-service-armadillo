package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import javax.annotation.CheckForNull;

@AutoValue
@JsonSerialize(as = ProfilesResponse.class)
public abstract class ProfilesResponse {
  @JsonProperty("available")
  public abstract List<String> available();

  @JsonProperty("current")
  @Nullable
  @CheckForNull
  public abstract String current();

  public static ProfilesResponse create(List<String> available, String current) {
    return new AutoValue_ProfilesResponse(available, current);
  }
}
