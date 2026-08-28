package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@AutoValue
public abstract class RequestAccessBody {
  @JsonProperty("requestId")
  @NotEmpty
  public abstract String getRequestId();

  @JsonProperty("data")
  @NotEmpty
  public abstract List<RequestData> getData();

  @JsonProperty("user")
  @NotEmpty
  public abstract String getUser();

  @JsonCreator
  public static RequestAccessBody create(
      @JsonProperty("requestId") String requestId,
      @JsonProperty("user") String user,
      @JsonProperty("data") List<RequestData> data) {
    return new AutoValue_RequestAccessBody(requestId, data, user);
  }
}
