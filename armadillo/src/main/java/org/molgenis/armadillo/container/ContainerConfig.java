package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DatashieldContainerConfig.class, name = "ds"),
  @JsonSubTypes.Type(value = DefaultContainerConfig.class, name = "default")
})
public interface ContainerConfig {

  @JsonProperty("name")
  String getName();

  @JsonProperty("image")
  String getImage();

  @JsonProperty("host")
  String getHost();

  @JsonProperty("port")
  Integer getPort();

  @JsonProperty("imageSize")
  @Nullable
  Long getImageSize();

  @JsonProperty("installDate")
  @Nullable
  String getInstallDate();

  @JsonProperty("lastImageId")
  @Nullable
  String getLastImageId();

  Map<String, Object> getSpecificContainerConfig();
}
