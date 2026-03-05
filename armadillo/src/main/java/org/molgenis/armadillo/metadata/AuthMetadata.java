package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AuthMetadata implements Persistable {
  @JsonProperty("client-id")
  public abstract String getClientId();

  @JsonProperty("client-secret")
  public abstract String getClientSecret();

  @JsonProperty("issuer-uri")
  public abstract String getIssuerUri();

  public static AuthMetadata create() {
    return new AutoValue_AuthMetadata("", "", "");
  }

  @JsonCreator
  public static AuthMetadata create(
      @JsonProperty("client-id") String newClientId,
      @JsonProperty("client-secret") String newClientSecret,
      @JsonProperty("issuer-uri") String newIssuerUri) {
    return new AutoValue_AuthMetadata(newClientId, newClientSecret, newIssuerUri);
  }
}
