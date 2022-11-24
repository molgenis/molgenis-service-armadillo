package org.molgenis.armadillo.metadata;

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

  @JsonCreator
  public static ProjectDetails create(
      @JsonProperty("name") String newName, @JsonProperty("users") Set<String> newUsers) {
    return new AutoValue_ProjectDetails(newName, newUsers != null ? newUsers : new HashSet<>());
  }
}
