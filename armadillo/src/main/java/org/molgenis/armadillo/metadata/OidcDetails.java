package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OidcDetails implements Persistable {

  @JsonProperty("issuerUri")
  @NotEmpty
  public abstract String getIssuerUri();

  @JsonProperty("clientId")
  @NotEmpty
  public abstract String getClientId();

  @JsonProperty("clientSecret")
  @NotEmpty
  public abstract String getClientSecret();

  /** Creates an empty instance used as a default when no config has been persisted yet. */
  public static OidcDetails create() {
    return new AutoValue_OidcDetails("", "", "");
  }

  @JsonCreator
  public static OidcDetails create(
      @JsonProperty("issuerUri") String issuerUri,
      @JsonProperty("clientId") String clientId,
      @JsonProperty("clientSecret") String clientSecret) {
    return new AutoValue_OidcDetails(issuerUri, clientId, clientSecret);
  }
}
