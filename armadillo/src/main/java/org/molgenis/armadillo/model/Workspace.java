package org.molgenis.armadillo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.ZonedDateTime;

@AutoValue
@JsonSerialize(as = Workspace.class)
public abstract class Workspace {

  @JsonProperty("name")
  public abstract String name();

  @JsonProperty("lastModified")
  public abstract ZonedDateTime lastModified();

  @JsonProperty("size")
  public abstract long size();

  abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setLastModified(ZonedDateTime lastModified);

    public abstract Builder setSize(long size);

    public abstract Workspace build();
  }

  public static Builder builder() {
    return new AutoValue_Workspace.Builder();
  }
}
