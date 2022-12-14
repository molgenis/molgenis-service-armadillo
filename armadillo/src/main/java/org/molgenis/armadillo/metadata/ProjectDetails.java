package org.molgenis.armadillo.metadata;

import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotEmpty;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProjectDetails {

  @JsonProperty("name")
  @NotEmpty
  public abstract String getName();

  @JsonProperty("users")
  public abstract Set<String> getUsers();

  @JsonProperty("profiles")
  public abstract Set<String> getProfiles();

  @JsonCreator
  public static ProjectDetails create(
      @JsonProperty("name") String newName,
      @JsonProperty("users") Set<String> newUsers,
      @JsonProperty("profiles") Set<String> newProfiles) {
    return new AutoValue_ProjectDetails(
        newName,
        newUsers != null ? newUsers : new HashSet<>(),
        newProfiles != null ? newProfiles : Set.of(DEFAULT));
  }
}
