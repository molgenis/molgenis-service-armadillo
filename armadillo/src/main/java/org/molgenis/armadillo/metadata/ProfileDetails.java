package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProfileDetails {
  public enum Status {
    RUNNING,
    STOPPED
  }

  @JsonProperty("name")
  public abstract String getName();

  @JsonProperty("image")
  public abstract String getImage();

  @JsonProperty("port")
  public abstract Integer getPort();

  @JsonProperty("whitelist")
  public abstract List<String> getWhitelist();

  @JsonProperty("seed")
  @Nullable
  public abstract String getSeed();

  @JsonProperty("status")
  @Nullable
  public abstract Status getStatus();

  @JsonCreator
  public static ProfileDetails create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("whitelist") List<String> newWhitelist,
      @JsonProperty("seed") String newSeed,
      @JsonProperty("status") Status newStatus) {
    return new AutoValue_ProfileDetails(
        newName, newImage, newPort, newWhitelist, newSeed, newStatus);
  }
}
