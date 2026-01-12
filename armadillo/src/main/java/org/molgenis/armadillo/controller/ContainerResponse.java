package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.ContainerInfo;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ContainerResponse {

  // 1. Define all fields as abstract methods
  public abstract String getType();

  public abstract String getName();

  public abstract String getImage();

  public abstract Integer getPort();

  @JsonProperty("specificContainerData")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Map<String, Object> getSpecificContainerData();

  @JsonProperty("dockerStatus")
  @Nullable
  public abstract ContainerInfo getContainerInfo();

  public static ContainerResponse create(ContainerConfig config, @Nullable ContainerInfo info) {
    return new AutoValue_ContainerResponse(
        config.getType(),
        config.getName(),
        config.getImage(),
        config.getPort(),
        config.getSpecificContainerConfig(),
        info);
  }
}
