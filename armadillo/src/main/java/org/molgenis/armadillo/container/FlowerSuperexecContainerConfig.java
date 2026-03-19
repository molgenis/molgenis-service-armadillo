package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName("flower-superexec")
// AutoValue requires redeclaring interface methods as abstract - suppress S1161
@SuppressWarnings("java:S1161")
public abstract class FlowerSuperexecContainerConfig
    implements ContainerConfig, OpenContainer, FlowerContainer {

  @Override
  public abstract String getName();

  @Override
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
  @Nullable
  public abstract String getVersionId();

  @Override
  @Nullable
  public abstract String getCreationDate();

  @Override
  @JsonIgnore
  public String getType() {
    return "flower-superexec";
  }

  @JsonCreator
  public static FlowerSuperexecContainerConfig create(
      @JsonProperty("name") String name,
      @JsonProperty("image") String image,
      @JsonProperty("host") @Nullable String host,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("imageSize") @Nullable Long imageSize,
      @JsonProperty("installDate") @Nullable String installDate,
      @JsonProperty("lastImageId") @Nullable String lastImageId,
      @JsonProperty("dockerArgs") @Nullable List<String> dockerArgs,
      @JsonProperty("dockerOptions") @Nullable Map<String, Object> dockerOptions,
      @JsonProperty("versionId") @Nullable String versionId,
      @JsonProperty("creationDate") @Nullable String creationDate) {

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
        .versionId(versionId)
        .creationDate(creationDate)
        .build();
  }

  public static FlowerSuperexecContainerConfig.Builder builder() {
    return new AutoValue_FlowerSuperexecContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder image(String image);

    public abstract Builder host(@Nullable String host);

    public abstract Builder port(@Nullable Integer port);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder dockerArgs(@Nullable List<String> dockerArgs);

    public abstract Builder dockerOptions(@Nullable Map<String, Object> dockerOptions);

    public abstract Builder versionId(@Nullable String versionId);

    public abstract Builder creationDate(@Nullable String creationDate);

    public abstract FlowerSuperexecContainerConfig build();
  }
}
