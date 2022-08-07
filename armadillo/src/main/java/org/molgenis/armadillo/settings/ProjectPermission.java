package org.molgenis.armadillo.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class ProjectPermission {
  @JsonProperty("email")
  abstract String getEmail();

  @JsonProperty("project")
  abstract String getProject();

  @JsonCreator
  public static ProjectPermission create(
      @JsonProperty("email") String email, @JsonProperty("project") String project) {
    return new AutoValue_ProjectPermission(email, project);
  }
}
