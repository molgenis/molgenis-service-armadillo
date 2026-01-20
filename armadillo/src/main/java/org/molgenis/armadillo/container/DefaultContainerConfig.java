package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.Builder;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName("default")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DefaultContainerConfig implements ContainerConfig {

  @Override
  @Nullable
  public abstract String getName();

  @Override
  @Nullable
  public abstract String getImage();

  @Override
  @Nullable
  public abstract String getHost();

  @Override
  @Nullable
  public abstract Integer getPort();

  @Override
  @Nullable
  public abstract Long getImageSize();

  @Override
  @Nullable
  public abstract String getInstallDate();

  @Override
  @Nullable
  public abstract String getLastImageId();

  @Override
  @Nullable
  public abstract List<String> getDockerArgs();

  @Override
  @Nullable
  public abstract Map<String, Object> getDockerOptions();

  @Override
  @JsonIgnore
  public String getType() {
    return "default";
  }

  @JsonCreator
  public static DefaultContainerConfig create(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("image") @Nullable String image,
      @JsonProperty("host") @Nullable String host,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("imageSize") @Nullable Long imageSize,
      @JsonProperty("installDate") @Nullable String installDate,
      @JsonProperty("lastImageId") @Nullable String lastImageId,
      @JsonProperty("dockerArgs") @Nullable List<String> dockerArgs,
      @JsonProperty("dockerOptions") @Nullable Map<String, Object> dockerOptions) {

    return builder()
        .name(name)
        .image(image)
        .host(host)
        .port(port)
        .imageSize(imageSize)
        .installDate(installDate)
        .lastImageId(lastImageId)
        .dockerArgs(dockerArgs)
        .dockerOptions(dockerOptions)
        .build();
  }

  public static DefaultContainerConfig.Builder builder() {
    return new AutoValue_DefaultContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(@Nullable String name);

    public abstract Builder image(@Nullable String image);

    public abstract Builder host(@Nullable String host);

    public abstract Builder port(@Nullable Integer port);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder dockerArgs(@Nullable List<String> dockerArgs);

    public abstract Builder dockerOptions(@Nullable Map<String, Object> dockerOptions);

    @Nullable
    abstract String getImage();

    @Nullable
    abstract String getHost();

    @Nullable
    abstract Integer getPort();

    abstract DefaultContainerConfig autoBuild();

    public DefaultContainerConfig build() {
      if (getImage() == null) image("library/r-base");
      if (getHost() == null) host("localhost");
      if (getPort() == null) port(6311);

      return autoBuild();
    }
  }

  @Override
  public Map<String, Object> getSpecificContainerOptions() {
    return Map.of();
  }
}
