package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName("flower-supernode")
// AutoValue requires redeclaring interface methods as abstract - suppress S1161 and S3038
@SuppressWarnings({"java:S1161", "java:S3038"})
public abstract class FlowerSuperNodeContainerConfig extends AbstractFlowerContainerConfig {

  @Nullable
  public abstract String getCaCertPath();

  @Nullable
  public abstract String getAuthPrivateKeyPath();

  @Override
  @JsonIgnore
  public String getType() {
    return "flower-supernode";
  }

  @JsonCreator
  public static FlowerSuperNodeContainerConfig create(
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
      @JsonProperty("caCertPath") @Nullable String caCertPath,
      @JsonProperty("authPrivateKeyPath") @Nullable String authPrivateKeyPath) {

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
        .caCertPath(caCertPath)
        .authPrivateKeyPath(authPrivateKeyPath)
        .build();
  }

  public static FlowerSuperNodeContainerConfig.Builder builder() {
    return new AutoValue_FlowerSuperNodeContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder extends AbstractFlowerContainerConfig.Builder<Builder> {

    public abstract Builder caCertPath(@Nullable String caCertPath);

    public abstract Builder authPrivateKeyPath(@Nullable String authPrivateKeyPath);

    @Nullable
    abstract String getCaCertPath();

    @Nullable
    abstract String getAuthPrivateKeyPath();

    abstract FlowerSuperNodeContainerConfig autoBuild();

    public FlowerSuperNodeContainerConfig build() {
      if (getCaCertPath() == null) caCertPath("data/system/flower/ca.crt");
      if (getAuthPrivateKeyPath() == null) authPrivateKeyPath("data/system/flower/credentials");
      return autoBuild();
    }
  }
}
