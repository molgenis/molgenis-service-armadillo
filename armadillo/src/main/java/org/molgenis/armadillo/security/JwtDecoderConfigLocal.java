package org.molgenis.armadillo.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

// in case oauth2 settings are missing a dummy JWT decoder exists because it is never used.
@Configuration
@ConditionalOnProperty(
    name = "spring.security.oauth2.resourceserver.opaquetoken.client-id",
    matchIfMissing = true,
    havingValue = "value_that_never_appears")
public class JwtDecoderConfigLocal {

  @Bean
  public JwtDecoder jwtDecoder() {
    return token -> {
      throw new UnsupportedOperationException("JWT dummy configuration for 'test'");
    };
  }
}
