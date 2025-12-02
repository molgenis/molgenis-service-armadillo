package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DefaultContainerConfig implements ContainerConfig {

  @JsonProperty("name")
  @NotEmpty
  public abstract String getName();

  @JsonProperty("image")
  @Nullable
  public abstract String getImage();

  @JsonProperty("host")
  @Nullable
  @NotEmpty
  public abstract String getHost();

  @JsonProperty("port")
  @Positive
  public abstract Integer getPort();

  @JsonCreator
  public static DefaultContainerConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort) {
    return new AutoValue_DefaultContainerConfig(
        newName, newImage, newHost != null ? newHost : "localhost", newPort);
  }

  public static DefaultContainerConfig createDefault() {
    return create("default", null, "localhost", 6311);
  }
}
