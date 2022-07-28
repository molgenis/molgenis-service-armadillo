package org.molgenis.armadillo.security;

import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Profile("!test")
@Configuration
@Import(UserDetailsServiceAutoConfiguration.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AuthConfig extends WebSecurityConfigurerAdapter {
  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/v3/api-docs*")
        .permitAll()
        .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
        .permitAll()
        .requestMatchers(toAnyEndpoint())
        .hasRole("SU")
        .anyRequest()
        .authenticated()
        .and()
        .csrf()
        .disable()
        .cors()
        .and()
        .httpBasic()
        .realmName("Armadillo")
        .and()
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        new JwtRolesExtractor(accessPermissionManager()));
    return jwtAuthenticationConverter;
  }

  @Bean
  public AccessStorageService accessPermissionManager() {
    return new AccessStorageService();
  }

  @Profile("development")
  @Bean
  /** Allow CORS requests, needed for swagger UI to work, if the development profile is active. */
  CorsConfigurationSource corsConfigurationSource() {
    return request -> ALLOW_CORS;
  }
}
