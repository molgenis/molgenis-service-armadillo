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
public abstract class DefaultContainerConfig extends AbstractContainerConfig {

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

  @JsonProperty("lastImageId")
  @Nullable
  public abstract String getLastImageId();

  @JsonProperty("imageSize")
  @Nullable
  public abstract Long getImageSize();

  @JsonProperty("InstallDate")
  @Nullable
  public abstract String getInstallDate();

  @JsonCreator
  public static DefaultContainerConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("lastImageId") @Nullable String newLastImageId,
      @JsonProperty("imageSize") @Nullable Long newImageSize,
      @JsonProperty("installDate") @Nullable String newInstallDate) {
    return new AutoValue_DefaultContainerConfig(
        newName,
        newImage,
        newHost != null ? newHost : "localhost",
        newPort,
        newLastImageId,
        newImageSize,
        newInstallDate);
  }

  public static DefaultContainerConfig createDefault() {
    return create("default", null, "localhost", 6311, null, null, null);
  }
}
