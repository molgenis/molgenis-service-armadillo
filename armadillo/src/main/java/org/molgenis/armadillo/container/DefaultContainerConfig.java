package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.Builder;
import jakarta.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DefaultContainerConfig extends AbstractContainerConfig {

  @JsonCreator
  public static DefaultContainerConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("lastImageId") @Nullable String newLastImageId,
      @JsonProperty("imageSize") @Nullable Long newImageSize,
      @JsonProperty("installDate") @Nullable String newInstallDate) {

    return builder()
        .name(newName)
        .image(newImage)
        .host(newHost != null ? newHost : "localhost")
        .port(newPort)
        .lastImageId(newLastImageId)
        .imageSize(newImageSize)
        .installDate(newInstallDate)
        .build();
  }

  public static DefaultContainerConfig createDefault() {
    return builder().name("default").host("localhost").port(6311).build();
  }

  public static DefaultContainerConfig.Builder builder() {
    return new AutoValue_DefaultContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder image(@Nullable String image);

    public abstract Builder host(String host);

    public abstract Builder port(Integer port);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract DefaultContainerConfig build();
  }

  @Override
  public Map<String, Object> getSpecificContainerConfig() {
    return Map.of();
  }
}
