package org.molgenis.datashield;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
public class AuthConfig extends WebSecurityConfigurerAdapter {
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2Login()
        .and()
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRolesExtractor());
    return jwtAuthenticationConverter;
  }

  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) ->
        authorities.stream()
            .filter(OidcUserAuthority.class::isInstance)
            .map(OidcUserAuthority.class::cast)
            .map(OidcUserAuthority::getIdToken)
            .map(OidcIdToken::getClaims)
            .flatMap(claims -> ((List<String>) claims.getOrDefault("roles", emptyList()))
                .stream()
                .map(name -> "ROLE_" + name))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
  }
}
