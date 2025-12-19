package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.util.Map;

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
