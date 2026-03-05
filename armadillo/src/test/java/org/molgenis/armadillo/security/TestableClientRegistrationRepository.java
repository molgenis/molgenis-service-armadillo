package org.molgenis.armadillo.security;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Test helper that bypasses OIDC discovery so tests don't make real HTTP calls. Lives in the
 * security package to access the package-private discoverAndLoad() method.
 */
public class TestableClientRegistrationRepository extends DynamicClientRegistrationRepository {

  @Override
  void discoverAndLoad(OidcConfig config) {
    forceLoad(
        ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId(config.clientId())
            .clientSecret(config.clientSecret())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://issuer.example.com/auth")
            .tokenUri("https://issuer.example.com/token")
            .jwkSetUri("https://issuer.example.com/jwks")
            .build());
  }
}
