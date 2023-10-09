package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.profile.ContainerInfo;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ProfileResponse {
  public abstract String getName();

  @Nullable // only required when docker enabled
  public abstract String getImage();

  public abstract String getHost();

  public abstract Integer getPort();

  public abstract Set<String> getPackageWhitelist();

  public abstract Set<String> getFunctionBlacklist();

  public abstract Map<String, String> getOptions();

  @JsonProperty("container")
  @Nullable // only present when docker management is enabled and Docker is online
  public abstract ContainerInfo getContainer();

  public static ProfileResponse create(ProfileConfig profileConfig, ContainerInfo containerInfo) {
    return new AutoValue_ProfileResponse(
        profileConfig.getName(),
        profileConfig.getImage(),
        profileConfig.getHost(),
        profileConfig.getPort(),
        profileConfig.getPackageWhitelist(),
        profileConfig.getFunctionBlacklist(),
        profileConfig.getOptions(),
        containerInfo);
  }
}
