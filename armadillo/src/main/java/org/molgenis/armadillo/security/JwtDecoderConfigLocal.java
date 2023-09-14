package org.molgenis.armadillo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

// in case of test there are no oidc then we will have dummy JWT
@Configuration
@ConditionalOnProperty("!{armadillo.oidc-permission-enabled}")
public class JwtDecoderConfigLocal {

  private static final Logger LOG = LoggerFactory.getLogger(JwtDecoderConfigLocal.class);

  @Bean
  public JwtDecoder jwtDecoder() {
    return token -> {
      throw new UnsupportedOperationException("JWT dummy configuration for 'test'");
    };
  }
}
