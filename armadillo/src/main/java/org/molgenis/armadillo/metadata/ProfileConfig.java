package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProfileConfig {
  // todo: decide if we want support images run on another server
  // @JsonProperty("host")
  // public abstract String getHost();

  @JsonProperty("name")
  public abstract String getName();

  @JsonProperty("image")
  public abstract String getImage();

  @JsonProperty("port")
  public abstract Integer getPort();

  @JsonProperty("whitelist")
  public abstract Set<String> getWhitelist();

  @JsonProperty("options")
  public abstract Map<String, String> getOptions();

  @JsonProperty("status")
  @Nullable
  public abstract ProfileStatus getStatus();

  @JsonCreator
  public static ProfileConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("whitelist") Set<String> newWhitelist,
      @JsonProperty("options") Map<String, String> newOptions,
      @JsonProperty("status") ProfileStatus newStatus) {
    return new AutoValue_ProfileConfig(
        newName,
        newImage,
        newPort,
        newWhitelist,
        newOptions != null ? newOptions : Map.of(),
        newStatus != null ? newStatus : null);
  }
}
