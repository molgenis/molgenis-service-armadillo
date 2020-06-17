package org.molgenis.datashield.jwt;

import static org.springframework.security.config.http.SessionCreationPolicy.ALWAYS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile({"development", "production"})
public class JWTConfig extends WebSecurityConfigurerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JWTConfig.class);

  final JwtRolesExtractor extractor;

  public JWTConfig(JwtRolesExtractor extractor) {
    this.extractor = extractor;
  }

  @Bean
  JwtAuthenticationConverter grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(extractor);
    return jwtAuthenticationConverter;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    LOGGER.info("Configuring JWT authentication.");
    http.authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .csrf()
        .disable()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
        .and()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(ALWAYS);
  }
}
