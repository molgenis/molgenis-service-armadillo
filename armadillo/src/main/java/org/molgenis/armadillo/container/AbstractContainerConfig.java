package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import org.molgenis.r.config.EnvironmentConfigProps;

public abstract class AbstractContainerConfig implements ContainerConfig {

  @JsonProperty("name")
  @NotEmpty
  public abstract String getName();

  @JsonProperty("image")
  @Nullable // only required when docker enabled
  public abstract String getImage();

  @JsonProperty("host")
  @Nullable // defaults to localhost
  @NotEmpty
  public abstract String getHost();

  @JsonProperty("port")
  @Positive
  public abstract Integer getPort();

  @JsonProperty("lastImageId")
  @Nullable
  public abstract String getLastImageId();

  @JsonProperty("imageSize")
  @Nullable
  public abstract Long getImageSize();

  @JsonProperty("InstallDate")
  @Nullable
  public abstract String getInstallDate();

  @Override
  @Nullable
  public OpenContainersConfig getOpenContainersConfig() {
    return null;
  }

  @Override
  @Nullable
  public UpdatableContainerConfig getUpdatableContainerConfig() {
    return null;
  }

  @Override
  @Nullable
  public Map<String, Object> getSpecificContainerConfig() {
    return Map.of();
  }

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    props.setImage(getImage());
    return props;
  }
}
