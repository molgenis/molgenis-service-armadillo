package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;

@AutoValue
public abstract class RequestData {
  @JsonProperty("table")
  @NotEmpty
  // TODO discuss: is table sufficient? This would then need to be a path to the table, e.g.
  // uncan/data/table.parquet
  // alternative: specify project and folder separately
  public abstract String getTable();

  @JsonProperty("variables")
  @NotEmpty
  public abstract String getVariables();

  @JsonCreator
  public static RequestData create(
      @JsonProperty("table") String table, @JsonProperty("variables") String variables) {
    return new AutoValue_RequestData(table, variables);
  }
}
