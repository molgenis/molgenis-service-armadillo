package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DatashieldContainerConfig.class, name = "ds"),
  @JsonSubTypes.Type(value = DefaultContainerConfig.class, name = "default")
})
public interface ContainerConfig {

  @JsonProperty("name")
  @Nullable
  String getName();

  @JsonProperty("image")
  @Nullable
  String getImage();

  @JsonProperty("host")
  @Nullable
  String getHost();

  @JsonProperty("port")
  @Nullable
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

  String getType();
}
