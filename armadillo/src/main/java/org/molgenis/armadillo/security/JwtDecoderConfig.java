package org.molgenis.armadillo.security;

import static org.springframework.security.oauth2.jwt.JwtClaimNames.AUD;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
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

@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver",
    value = {"jwt.issuer-uri", "opaquetoken.client-id"})
@Configuration
public class JwtDecoderConfig {

  private static final Logger LOG = LoggerFactory.getLogger(JwtDecoderConfig.class);

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  @Bean
  public JwtDecoder jwtDecoder(
      OAuth2ResourceServerProperties properties, KeyPair resourceTokenKeyPair) {
    try {
      String issuerUri = properties.getJwt().getIssuerUri();
      NimbusJwtDecoder externalDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

      var audienceValidator =
          new JwtClaimValidator<Collection<String>>(
              AUD, aud -> aud != null && aud.contains(properties.getOpaquetoken().getClientId()));
      OAuth2TokenValidator<Jwt> jwtValidator =
          new DelegatingOAuth2TokenValidator<>(
              JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator);

      externalDecoder.setJwtValidator(jwtValidator);

      NimbusJwtDecoder internalDecoder =
          NimbusJwtDecoder.withPublicKey((RSAPublicKey) resourceTokenKeyPair.getPublic()).build();
      OAuth2TokenValidator<Jwt> internalValidator =
          new DelegatingOAuth2TokenValidator<>(
              new JwtTimestampValidator(),
              new JwtIssuerValidator("armadillo-internal"));
      internalDecoder.setJwtValidator(internalValidator);

      return token -> {
        try {
          return internalDecoder.decode(token);
        } catch (JwtException e) {
          return externalDecoder.decode(token);
        }
      };
    } catch (Exception e) {
      if ("offline".equals(activeProfile)) {
        // allow offline development
        LOG.error("Couldn't configure JWT decoder", e);
        return token -> {
          throw new UnsupportedOperationException(
              "JWT configuration failed, please check the logs. Probably the auth server is offline?");
        };
      } else {
        throw e;
      }
    }
  }
}
