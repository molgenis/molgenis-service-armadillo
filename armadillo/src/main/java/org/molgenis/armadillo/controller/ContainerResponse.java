package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
// Keep if needed for other generic purposes, otherwise can be removed
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.ContainerInfo;

// import org.molgenis.armadillo.container.DatashieldContainerConfig; // REMOVED: No longer needed
// for casting
// REMOVED: No longer needed (unless part of

// a generic property)

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ContainerResponse {

  public abstract String getName();

  @Nullable
  public abstract String getImage();

  public abstract String getHost();

  public abstract Integer getPort();

  @Nullable
  @JsonProperty("lastImageId")
  public abstract String getLastImageId();

  @JsonProperty("imageSize")
  @Nullable
  public abstract Long getImageSize();

  @JsonProperty("installDate")
  @Nullable
  public abstract String getInstallDate();

  // --- CONTAINER STATUS FIELD ---

  @JsonProperty("container")
  @Nullable // only present when docker management is enabled and Docker is online
  public abstract ContainerInfo getContainer();

  // --- GENERIC FIELD FOR SPECIFIC/UNIQUE DATA ---

  /**
   * Contains all properties that are unique to the specific container type (e.g., packageWhitelist,
   * autoUpdate for Datashield).
   */
  @JsonProperty("specificContainerData")
  public abstract Map<String, Object> getSpecificContainerData();

  public static ContainerResponse create(
      ContainerConfig containerConfig, ContainerInfo containerInfo) {

    return new AutoValue_ContainerResponse(
        containerConfig.getName(),
        containerConfig.getImage(),
        containerConfig.getHost(),
        containerConfig.getPort(),
        containerConfig.getLastImageId(),
        containerConfig.getImageSize(),
        containerConfig.getInstallDate(),
        containerInfo,
        containerConfig.getSpecificContainerData());
  }
}
