package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonTypeName("v6")
public abstract class Vantage6ContainerConfig implements ContainerConfig, OpenContainer {

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
  public abstract String getVersionId();

  @Override
  @Nullable
  public abstract String getCreationDate();

  @Override
  @Nullable
  public abstract List<String> getDockerArgs();

  @Override
  @Nullable
  public abstract Map<String, Object> getDockerOptions();

  @JsonProperty("serverUrl")
  @Nullable
  public abstract String getServerUrl();

  @JsonProperty("apiKey")
  @Nullable
  public abstract String getApiKey();

  @JsonProperty("collaborationId")
  @Nullable
  public abstract Integer getCollaborationId();

  @JsonProperty("encryptionKey")
  @Nullable
  public abstract String getEncryptionKey();

  @JsonProperty("allowedAlgorithms")
  @Nullable
  public abstract Set<String> getAllowedAlgorithms();

  @JsonProperty("allowedAlgorithmStores")
  @Nullable
  public abstract Set<String> getAllowedAlgorithmStores();

  @JsonProperty("authorizedProjects")
  @Nullable
  public abstract Set<String> getAuthorizedProjects();

  @Override
  @JsonIgnore
  public String getType() {
    return "v6";
  }

  @JsonCreator
  public static Vantage6ContainerConfig create(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("image") @Nullable String image,
      @JsonProperty("host") @Nullable String host,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("lastImageId") @Nullable String lastImageId,
      @JsonProperty("imageSize") @Nullable Long imageSize,
      @JsonProperty("installDate") @Nullable String installDate,
      @JsonProperty("versionId") @Nullable String versionId,
      @JsonProperty("creationDate") @Nullable String creationDate,
      @JsonProperty("serverUrl") @Nullable String serverUrl,
      @JsonProperty("apiKey") @Nullable String apiKey,
      @JsonProperty("collaborationId") @Nullable Integer collaborationId,
      @JsonProperty("encryptionKey") @Nullable String encryptionKey,
      @JsonProperty("allowedAlgorithms") @Nullable Set<String> allowedAlgorithms,
      @JsonProperty("allowedAlgorithmStores") @Nullable Set<String> allowedAlgorithmStores,
      @JsonProperty("authorizedProjects") @Nullable Set<String> authorizedProjects,
      @JsonProperty("dockerArgs") @Nullable List<String> dockerArgs,
      @JsonProperty("dockerOptions") @Nullable Map<String, Object> dockerOptions) {

    return builder()
        .name(name)
        .image(image)
        .host(host)
        .port(port)
        .lastImageId(lastImageId)
        .imageSize(imageSize)
        .installDate(installDate)
        .versionId(versionId)
        .creationDate(creationDate)
        .serverUrl(serverUrl)
        .apiKey(apiKey)
        .collaborationId(collaborationId)
        .encryptionKey(encryptionKey)
        .allowedAlgorithms(allowedAlgorithms)
        .allowedAlgorithmStores(allowedAlgorithmStores)
        .authorizedProjects(authorizedProjects)
        .dockerArgs(dockerArgs)
        .dockerOptions(dockerOptions)
        .build();
  }

  public static Vantage6ContainerConfig createDefault() {
    return builder().name("default").build();
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_Vantage6ContainerConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(@Nullable String name);

    public abstract Builder image(@Nullable String image);

    public abstract Builder host(@Nullable String host);

    public abstract Builder port(@Nullable Integer port);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder versionId(@Nullable String versionId);

    public abstract Builder creationDate(@Nullable String creationDate);

    public abstract Builder serverUrl(@Nullable String serverUrl);

    public abstract Builder apiKey(@Nullable String apiKey);

    public abstract Builder collaborationId(@Nullable Integer collaborationId);

    public abstract Builder encryptionKey(@Nullable String encryptionKey);

    public abstract Builder allowedAlgorithms(@Nullable Set<String> allowedAlgorithms);

    public abstract Builder allowedAlgorithmStores(@Nullable Set<String> allowedAlgorithmStores);

    public abstract Builder authorizedProjects(@Nullable Set<String> authorizedProjects);

    public abstract Builder dockerArgs(@Nullable List<String> dockerArgs);

    public abstract Builder dockerOptions(@Nullable Map<String, Object> dockerOptions);

    @Nullable
    abstract String getImage();

    @Nullable
    abstract String getHost();

    @Nullable
    abstract Integer getPort();

    @Nullable
    abstract Set<String> getAllowedAlgorithms();

    @Nullable
    abstract Set<String> getAllowedAlgorithmStores();

    @Nullable
    abstract Set<String> getAuthorizedProjects();

    abstract Vantage6ContainerConfig autoBuild();

    public Vantage6ContainerConfig build() {
      if (getImage() == null) image("molgenis/armadillo-v6-bridge");
      if (getHost() == null) host("localhost");
      if (getPort() == null) port(8081);
      if (getAllowedAlgorithms() == null) allowedAlgorithms(Set.of());
      if (getAllowedAlgorithmStores() == null) allowedAlgorithmStores(Set.of());
      if (getAuthorizedProjects() == null) authorizedProjects(Set.of());

      return autoBuild();
    }
  }
}
