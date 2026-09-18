package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName("flower-superexec")
// AutoValue requires redeclaring interface methods as abstract - suppress S1161 and S3038
@SuppressWarnings({"java:S1161", "java:S3038"})
public abstract class FlowerSuperExecContainerConfig extends AbstractFlowerContainerConfig {

  @Nullable
  public abstract String getFabWhitelistPath();

  @Override
  @JsonIgnore
  public String getType() {
    return "flower-superexec";
  }

  @JsonCreator
  public static FlowerSuperExecContainerConfig create(
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
      @JsonProperty("creationDate") @Nullable String creationDate,
      @JsonProperty("fabWhitelistPath") @Nullable String fabWhitelistPath) {

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
        .fabWhitelistPath(fabWhitelistPath)
        .build();
  }

  public static FlowerSuperExecContainerConfig.Builder builder() {
    return new AutoValue_FlowerSuperExecContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder extends AbstractFlowerContainerConfig.Builder<Builder> {

    public abstract Builder fabWhitelistPath(@Nullable String fabWhitelistPath);

    abstract String getName();

    @Nullable
    abstract String getFabWhitelistPath();

    abstract FlowerSuperExecContainerConfig autoBuild();

    public FlowerSuperExecContainerConfig build() {
      if (getFabWhitelistPath() == null) {
        fabWhitelistPath("data/system/flower/" + getName() + "-fab-whitelist.yaml");
      }
      return autoBuild();
    }
  }
}
