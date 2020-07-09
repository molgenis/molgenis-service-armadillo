package org.molgenis.armadillo.info;

import java.util.Map;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.stereotype.Component;

@Component
public class AuthInfoContributor implements InfoContributor {

  private final String clientId;
  private final String issuerUri;

  public AuthInfoContributor(OAuth2ResourceServerProperties properties) {
    this.clientId = properties.getOpaquetoken().getClientId();
    this.issuerUri = properties.getJwt().getIssuerUri();
  }

  @Override
  public void contribute(Builder builder) {
    builder.withDetail("auth", Map.of("clientId", clientId, "issuerUri", issuerUri));
  }
}
