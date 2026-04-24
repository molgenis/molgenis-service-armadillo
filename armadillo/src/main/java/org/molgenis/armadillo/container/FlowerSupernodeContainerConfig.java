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
public abstract class FlowerSupernodeContainerConfig
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

  @Nullable
  public abstract String getTrustedEntitiesPath();

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
  public static FlowerSupernodeContainerConfig create(
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
      @JsonProperty("trustedEntitiesPath") @Nullable String trustedEntitiesPath,
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
        .trustedEntitiesPath(trustedEntitiesPath)
        .caCertPath(caCertPath)
        .authPrivateKeyPath(authPrivateKeyPath)
        .build();
  }

  public static FlowerSupernodeContainerConfig.Builder builder() {
    return new AutoValue_FlowerSupernodeContainerConfig.Builder();
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

    public abstract Builder trustedEntitiesPath(@Nullable String trustedEntitiesPath);

    public abstract Builder caCertPath(@Nullable String caCertPath);

    public abstract Builder authPrivateKeyPath(@Nullable String authPrivateKeyPath);

    @Nullable
    abstract String getTrustedEntitiesPath();

    @Nullable
    abstract String getCaCertPath();

    @Nullable
    abstract String getAuthPrivateKeyPath();

    abstract FlowerSupernodeContainerConfig autoBuild();

    public FlowerSupernodeContainerConfig build() {
      if (getTrustedEntitiesPath() == null)
        trustedEntitiesPath("data/system/flower/trusted-entities.yaml");
      if (getCaCertPath() == null) caCertPath("data/system/flower/ca.crt");
      if (getAuthPrivateKeyPath() == null) authPrivateKeyPath("data/system/flower/credentials");
      return autoBuild();
    }
  }
}
