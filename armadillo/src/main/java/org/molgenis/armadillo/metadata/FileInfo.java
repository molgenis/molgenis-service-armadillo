package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FileInfo {
  @JsonProperty("id")
  @NotEmpty
  public abstract String getId();

  @JsonProperty("name")
  @NotEmpty
  public abstract String getName();

  @JsonCreator
  public static FileInfo create(
      @JsonProperty("id") String newId, @JsonProperty("name") String newName) {
    return new AutoValue_FileInfo(newId, newName);
  }
}
