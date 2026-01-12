package org.molgenis.armadillo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
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
  @JsonSubTypes.Type(value = ContainerResponse.DefaultResponse.class, name = "default"),
  @JsonSubTypes.Type(value = ContainerResponse.DatashieldResponse.class, name = "ds")
})
public sealed interface ContainerResponse {
  String type();

  String name();

  String image();

  Integer port();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record DefaultResponse(
      String type,
      String name,
      String image,
      Integer port,
      @JsonProperty("dockerStatus") @Nullable ContainerInfo containerInfo)
      implements ContainerResponse {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record DatashieldResponse(
      String type,
      String name,
      String image,
      Integer port,
      @JsonProperty("specificContainerData") Map<String, Object> specificContainerData,
      @JsonProperty("dockerStatus") @Nullable ContainerInfo containerInfo)
      implements ContainerResponse {}

  static ContainerResponse create(ContainerConfig config, @Nullable ContainerInfo info) {
    if (config instanceof DatashieldContainerConfig ds) {
      return new DatashieldResponse(
          ds.getType(),
          ds.getName(),
          ds.getImage(),
          ds.getPort(),
          ds.getSpecificContainerConfig(),
          info);
    }
    return new DefaultResponse(
        config.getType(), config.getName(), config.getImage(), config.getPort(), info);
  }
}
