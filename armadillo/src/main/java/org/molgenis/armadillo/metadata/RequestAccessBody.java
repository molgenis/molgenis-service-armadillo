package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;

@AutoValue
public abstract class RequestAccessBody {
  @JsonProperty("requestId")
  @NotEmpty
  public abstract String getRequestId();

  @JsonProperty("user")
  @NotEmpty
  public abstract String getUser();

  @JsonProperty("table")
  @NotEmpty
  // TODO discuss: is table sufficient? This would then need to be a path to the table, e.g.
  // uncan/data/table.parquet
  // alternative: specify project and folder separately
  public abstract String getTable();

  @JsonProperty("variables")
  @NotEmpty
  public abstract ArrayList<String> getVariables();

  @JsonCreator
  public static RequestAccessBody create(
      @JsonProperty("requestId") String requestId,
      @JsonProperty("user") String user,
      @JsonProperty("table") String table,
      @JsonProperty("variables") ArrayList<String> variables) {
    return new AutoValue_RequestAccessBody(requestId, user, table, variables);
  }
}
