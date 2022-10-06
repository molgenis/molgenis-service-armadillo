package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileStatus;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ProfileResponse {
  public abstract String getName();

  @Nullable // only required when docker enabled
  public abstract String getImage();

  public abstract String getHost();

  public abstract Integer getPort();

  public abstract Set<String> getWhitelist();

  public abstract Map<String, String> getOptions();

  @JsonProperty("container_status")
  @Nullable // only present when docker management is enabled
  public abstract ProfileStatus getStatus();

  public static ProfileResponse create(ProfileConfig profileConfig, ProfileStatus status) {
    return new AutoValue_ProfileResponse(
        profileConfig.getName(),
        profileConfig.getImage(),
        profileConfig.getHost(),
        profileConfig.getPort(),
        profileConfig.getWhitelist(),
        profileConfig.getOptions(),
        status);
  }
}
