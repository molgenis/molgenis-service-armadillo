package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Set;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class ProjectDetails {
  @JsonProperty("name")
  public abstract String getName();

  @JsonProperty("users")
  @Nullable
  public abstract Set<String> getUsers();

  @JsonCreator
  public static ProjectDetails create(
      @JsonProperty("name") String newName, @JsonProperty("users") Set<String> newUsers) {
    return new AutoValue_ProjectDetails(newName, newUsers);
  }
}
