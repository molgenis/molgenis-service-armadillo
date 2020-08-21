package org.molgenis.armadillo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Date;

@AutoValue
@JsonSerialize(as = Workspace.class)
public abstract class Workspace {

  @JsonProperty("name")
  public abstract String name();

  @JsonProperty("lastModified")
  public abstract Instant lastModified();

  @JsonProperty("size")
  public abstract long size();

  @JsonProperty("ETag")
  public abstract String eTag();

  abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setLastModified(Instant lastModified);

    public Builder setLastModified(Date lastModified) {
      return setLastModified(Instant.ofEpochMilli(lastModified.getTime()));
    }

    public abstract Builder setSize(long size);

    public abstract Builder setETag(String etag);

    public abstract Workspace build();
  }

  public static Builder builder() {
    return new AutoValue_Workspace.Builder();
  }
}
