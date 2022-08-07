package org.molgenis.armadillo.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Set;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class UserDetails {
  @JsonProperty("email")
  abstract String getEmail();

  @JsonProperty("firstName")
  @Nullable
  public abstract String getFirstName();

  @JsonProperty("lastName")
  @Nullable
  public abstract String getLastName();

  @JsonProperty("institution")
  @Nullable
  public abstract String getInstitution();

  @JsonProperty("projects")
  @Nullable
  public abstract Set<String> getProjects();

  @JsonCreator
  public static UserDetails create(
      @JsonProperty("email") String newEmail,
      @JsonProperty("firstName") String newFirstName,
      @JsonProperty("lastName") String newLastName,
      @JsonProperty("institution") String newInstitution,
      @JsonProperty("projects") Set<String> newProjects) {
    return new AutoValue_UserDetails(
        newEmail, newFirstName, newLastName, newInstitution, newProjects);
  }
}
