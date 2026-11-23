package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@AutoValue
public abstract class RequestAccessBody {
  @JsonProperty("data")
  @NotEmpty
  public abstract List<RequestData> getData();

  @JsonProperty("user")
  @NotEmpty
  public abstract String getUser();

  @JsonProperty("apiVersion")
  @NotEmpty
  public abstract String getApiVersion();

  @JsonCreator
  public static RequestAccessBody create(
      @JsonProperty("user") String user,
      @JsonProperty("data") List<RequestData> data,
      @JsonProperty("apiVersion") String apiVersion) {
    return new AutoValue_RequestAccessBody(data, user, apiVersion);
  }
}
