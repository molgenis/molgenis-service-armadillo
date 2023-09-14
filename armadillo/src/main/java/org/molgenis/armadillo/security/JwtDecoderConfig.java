package org.molgenis.armadillo.security;

import static org.springframework.security.oauth2.jwt.JwtClaimNames.AUD;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@ConditionalOnProperty(value = "spring.security.oauth2.client.registration.molgenis.client-id")
public class JwtDecoderConfig {

  private static final Logger LOG = LoggerFactory.getLogger(JwtDecoderConfig.class);

  @Value("${spring.profiles.active:default}")
  private String activeProfile = "default";

  @Bean
  public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
    try {
      String issuerUri = properties.getJwt().getIssuerUri();
      NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

      var audienceValidator =
          new JwtClaimValidator<Collection<String>>(
              AUD, aud -> aud != null && aud.contains(properties.getOpaquetoken().getClientId()));
      OAuth2TokenValidator<Jwt> jwtValidator =
          new DelegatingOAuth2TokenValidator<>(
              JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator);

      jwtDecoder.setJwtValidator(jwtValidator);
      return jwtDecoder;
    } catch (Exception e) {
      // allow offline development
      LOG.error("Couldn't configure JWT decoder", e);
      return token -> {
        throw new UnsupportedOperationException(
            "JWT configuration failed, please check the logs. Probably the auth server is offline?");
      };
    }
  }
}
