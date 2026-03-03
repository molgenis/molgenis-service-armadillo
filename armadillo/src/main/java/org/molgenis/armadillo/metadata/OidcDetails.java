package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OidcDetails {
  @JsonProperty("clientId")
  @NotEmpty
  public abstract String getClientId();

  @JsonProperty("clientSecret")
  @NotEmpty
  public abstract String getClientSecret();

  @JsonProperty("authServerUri")
  @NotEmpty
  public abstract String getAuthServerUri();

  @JsonCreator
  public static OidcDetails create(
      @JsonProperty("authServerUri") String authServerUri,
      @JsonProperty("clientId") String clientId,
      @JsonProperty("clientSecret") String clientSecret) {
    return new AutoValue_OidcDetails(authServerUri, clientId, clientSecret);
  }
}
