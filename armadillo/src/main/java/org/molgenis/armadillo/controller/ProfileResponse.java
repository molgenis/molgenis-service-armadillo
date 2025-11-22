package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.UpdateSchedule;
import org.molgenis.armadillo.profile.ContainerInfo;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ProfileResponse {
  public abstract String getName();

  @Nullable // only required when docker enabled
  public abstract String getImage();

  @Nullable
  @JsonProperty("autoUpdate")
  public abstract Boolean getAutoUpdate();

  @Nullable
  @JsonProperty("updateSchedule")
  public abstract UpdateSchedule getUpdateSchedule();

  public abstract String getHost();

  public abstract Integer getPort();

  public abstract Set<String> getPackageWhitelist();

  public abstract Set<String> getFunctionBlacklist();

  public abstract Map<String, String> getOptions();

  @JsonProperty("container")
  @Nullable // only present when docker management is enabled and Docker is online
  public abstract ContainerInfo getContainer();

  @Nullable
  @JsonProperty("lastImageId") // Add this line to include lastImageId in the response
  public abstract String getLastImageId();

  @Nullable
  @JsonProperty("versionId") //
  public abstract String getVersionId();

  @JsonProperty("imageSize")
  @Nullable
  public abstract Long getImageSize();

  @JsonProperty("creationDate")
  @Nullable
  public abstract String getCreationDate();

  @JsonProperty("installDate")
  @Nullable
  public abstract String getInstallDate();

  public static ProfileResponse create(
      ContainerConfig containerConfig, ContainerInfo containerInfo) {
    return new AutoValue_ProfileResponse(
        containerConfig.getName(),
        containerConfig.getImage(),
        containerConfig.getAutoUpdate(),
        containerConfig.getUpdateSchedule(),
        containerConfig.getHost(),
        containerConfig.getPort(),
        containerConfig.getPackageWhitelist(),
        containerConfig.getFunctionBlacklist(),
        containerConfig.getOptions(),
        containerInfo,
        containerConfig.getLastImageId(),
        containerConfig.getVersionId(),
        containerConfig.getImageSize(),
        containerConfig.getCreationDate(),
        containerConfig.getInstallDate());
  }
}
