package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.ResourceTokenService.INTERNAL_ISSUER;
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

@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver",
    value = {"jwt.issuer-uri", "opaquetoken.client-id"})
@Configuration
public class JwtDecoderConfig {

  static Logger LOG = LoggerFactory.getLogger(JwtDecoderConfig.class);

  @Value("${spring.containers.active:default}")
  private String activeContainer;

  JwtClaimValidator<Collection<String>> getAudienceValidator(
      OAuth2ResourceServerProperties properties) {
    return new JwtClaimValidator<>(
        AUD, aud -> aud != null && aud.contains(properties.getOpaquetoken().getClientId()));
  }

  OAuth2TokenValidator<Jwt> getJwtValidator(
      String issuerUri, JwtClaimValidator<Collection<String>> audienceValidator) {
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator);
  }

  NimbusJwtDecoder getInternalDecoder(ResourceTokenService resourceTokenService) {
    NimbusJwtDecoder internalDecoder =
        NimbusJwtDecoder.withPublicKey(resourceTokenService.getPublicKey()).build();
    OAuth2TokenValidator<Jwt> internalValidator = getInternalValidator();
    internalDecoder.setJwtValidator(internalValidator);
    return internalDecoder;
  }

  NimbusJwtDecoder getExternalDecoder(String issuerUri, OAuth2ResourceServerProperties properties) {
    var audienceValidator = getAudienceValidator(properties);
    OAuth2TokenValidator<Jwt> jwtValidator = getJwtValidator(issuerUri, audienceValidator);
    NimbusJwtDecoder externalDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
    externalDecoder.setJwtValidator(jwtValidator);
    return externalDecoder;
  }

  OAuth2TokenValidator<Jwt> getInternalValidator() {
    return new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(), new JwtIssuerValidator(INTERNAL_ISSUER));
  }

  @Bean
  public JwtDecoder jwtDecoder(
      OAuth2ResourceServerProperties properties, ResourceTokenService resourceTokenService) {
    try {
      String issuerUri = properties.getJwt().getIssuerUri();
      NimbusJwtDecoder externalDecoder = getExternalDecoder(issuerUri, properties);
      NimbusJwtDecoder internalDecoder = getInternalDecoder(resourceTokenService);

      return token -> {
        try {
          return internalDecoder.decode(token);
        } catch (JwtException e) {
          return externalDecoder.decode(token);
        }
      };
    } catch (Exception e) {
      if ("offline".equals(activeContainer)) {
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
