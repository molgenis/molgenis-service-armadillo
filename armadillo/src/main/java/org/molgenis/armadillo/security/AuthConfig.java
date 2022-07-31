package org.molgenis.armadillo.security;

import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Profile("!test")
@Import(UserDetailsServiceAutoConfiguration.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AuthConfig {
  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();

  @Configuration
  @EnableWebSecurity(debug = true)
  @Order(1)
  // first check against JWT, but only if header is set
  public static class JwtConfig extends WebSecurityConfigurerAdapter {

    AccessStorageService accessStorageService;

    public JwtConfig(AccessStorageService accessStorageService) {
      this.accessStorageService = accessStorageService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.requestMatcher(new RequestHeaderRequestMatcher("Authorization"))
          .authorizeRequests()
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
          new JwtRolesExtractor(accessStorageService));
      return jwtAuthenticationConverter;
    }
  }

  @Configuration
  @EnableWebSecurity(debug = true)
  @Order(2)
  // otherwise we gonna offer sign in
  public static class LoginConfig extends WebSecurityConfigurerAdapter {
    AccessStorageService accessStorageService;

    public LoginConfig(AccessStorageService accessStorageService) {
      this.accessStorageService = accessStorageService;
    }

    public void configure(WebSecurity web) {
      // permit swagger and ui
      web.ignoring()
          .antMatchers("/v3/**", "/swagger-ui/**", "/ui/**", "/swagger-ui.html", "/oauth2/*");
      return;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.authorizeRequests()
          // permit health monitoring endpoints
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
          .permitAll()
          // anything else should be authenticated
          .requestMatchers(toAnyEndpoint())
          .hasAnyRole()
          .anyRequest()
          .authenticated()
          // settings to be applied
          .and()
          .csrf()
          .disable()
          .cors()
          // force oauth2login (unless bearer token, see other config)
          .and()
          .oauth2Login(
              oauth2Login ->
                  oauth2Login.userInfoEndpoint(
                      userInfoEndpoint ->
                          userInfoEndpoint.userAuthoritiesMapper(this.userAuthoritiesMapper())));
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
      return (authorities) -> {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        authorities.forEach(
            authority -> {
              OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;

              Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
              mappedAuthorities.addAll(
                  getAuthoritiesForEmail(
                      accessStorageService, (String) userAttributes.get("email")));
            });

        return mappedAuthorities;
      };
    }
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

  public static Collection<SimpleGrantedAuthority> getAuthoritiesForEmail(
      AccessStorageService accessStorageService, String email) {
    List<String> authorizedProjects = accessStorageService.getGrantsForEmail(email);
    return authorizedProjects.stream()
        .map(
            project ->
                "administrators".equals(project) ? "ROLE_SU" : "ROLE_" + project + "_RESEARCHER")
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
