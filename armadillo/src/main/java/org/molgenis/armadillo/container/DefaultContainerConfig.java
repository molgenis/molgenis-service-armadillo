package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.Builder;
import jakarta.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DefaultContainerConfig implements ContainerConfig {

  @JsonCreator
  public static DefaultContainerConfig create(
      String name,
      String image,
      String host,
      Integer port,
      @Nullable Long imageSize,
      @Nullable String installDate,
      @Nullable String lastImageId) {

    return builder()
        .name(name)
        .image(image)
        .host(host)
        .port(port)
        .imageSize(imageSize)
        .installDate(installDate)
        .lastImageId(lastImageId)
        .build();
  }

  public static DefaultContainerConfig.Builder builder() {
    return new AutoValue_DefaultContainerConfig.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder image(String image);

    public abstract Builder host(String host);

    public abstract Builder port(Integer port);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract DefaultContainerConfig build();
  }

  @Override
  public Map<String, Object> getSpecificContainerConfig() {
    return Map.of();
  }
}
