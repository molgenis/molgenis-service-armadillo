package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OidcDetails implements Persistable {

  /**
   * Nullable here because OidcDetails doubles as a persistence model that may be empty on first
   * startup. Validation is enforced at the HTTP boundary via @Valid in the controller.
   */
  @JsonProperty("issuerUri")
  @Nullable
  public abstract String getIssuerUri();

  @JsonProperty("clientId")
  @Nullable
  public abstract String getClientId();

  @JsonProperty("clientSecret")
  @Nullable
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
