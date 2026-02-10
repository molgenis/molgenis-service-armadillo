package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Set;

@AutoValue
public abstract class Vantage6Token {

  @JsonProperty("id")
  public abstract String getId();

  @JsonProperty("containerName")
  public abstract String getContainerName();

  @JsonProperty("authorizedProjects")
  public abstract Set<String> getAuthorizedProjects();

  @JsonProperty("createdAt")
  @Nullable
  public abstract String getCreatedAt();

  @JsonProperty("description")
  @Nullable
  public abstract String getDescription();

  @JsonCreator
  public static Vantage6Token create(
      @JsonProperty("id") String id,
      @JsonProperty("containerName") String containerName,
      @JsonProperty("authorizedProjects") Set<String> authorizedProjects,
      @JsonProperty("createdAt") @Nullable String createdAt,
      @JsonProperty("description") @Nullable String description) {
    return new AutoValue_Vantage6Token(
        id,
        containerName,
        authorizedProjects != null ? authorizedProjects : Set.of(),
        createdAt,
        description);
  }
}
