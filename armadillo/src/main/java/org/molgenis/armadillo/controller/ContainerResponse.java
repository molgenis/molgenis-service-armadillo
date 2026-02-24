package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.ContainerInfo;
import org.molgenis.armadillo.container.DatashieldContainerConfig;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ContainerResponse.DefaultResponse.class, name = "vanilla"),
  @JsonSubTypes.Type(value = ContainerResponse.DatashieldResponse.class, name = "ds")
})
public sealed interface ContainerResponse {
  String name();

  String type();

  String image();

  Integer port();

  @Nullable
  Long imageSize();

  @Nullable
  String installDate();

  @Nullable
  String lastImageId();

  @Nullable
  List<String> dockerArgs();

  @Nullable
  Map<String, Object> dockerOptions();

  record DefaultResponse(
      String type,
      String name,
      String image,
      Integer port,
      @Nullable Long imageSize,
      @Nullable String installDate,
      @Nullable String lastImageId,
      @Nullable List<String> dockerArgs,
      @Nullable Map<String, Object> dockerOptions,
      @JsonProperty("dockerStatus") @Nullable ContainerInfo containerInfo)
      implements ContainerResponse {}

  record DatashieldResponse(
      String type,
      String name,
      String image,
      Integer port,
      @Nullable Long imageSize,
      @Nullable String installDate,
      @Nullable String lastImageId,
      @Nullable List<String> dockerArgs,
      @Nullable Map<String, Object> dockerOptions,
      @Nullable String versionId,
      @Nullable String creationDate,
      @Nullable Map<String, Object> specificContainerOptions,
      @JsonProperty("dockerStatus") @Nullable ContainerInfo containerInfo)
      implements ContainerResponse {}

  static ContainerResponse create(ContainerConfig config, @Nullable ContainerInfo info) {
    if (config instanceof DatashieldContainerConfig ds) {
      return new DatashieldResponse(
          ds.getType(),
          ds.getName(),
          ds.getImage(),
          ds.getPort(),
          ds.getImageSize(),
          ds.getInstallDate(),
          ds.getLastImageId(),
          ds.getDockerArgs(),
          ds.getDockerOptions(),
          ds.getVersionId(),
          ds.getCreationDate(),
          ds.getSpecificContainerOptions(),
          info);
    }

    return new DefaultResponse(
        config.getType(),
        config.getName(),
        config.getImage(),
        config.getPort(),
        config.getImageSize(),
        config.getInstallDate(),
        config.getLastImageId(),
        config.getDockerArgs(),
        config.getDockerOptions(),
        info);
  }
}
