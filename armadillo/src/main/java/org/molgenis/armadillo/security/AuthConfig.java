package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.AccessService;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Profile("!test")
@Import(UserDetailsServiceAutoConfiguration.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
// we have three configs that enable jwt, formLogin and oauth2Login respectively.
// they are ordered, so jwt config is most dominant and oauth2Login least dominant
// in 'test' profile they are not enabled
public class AuthConfig {
  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();

  @Configuration
  @EnableWebSecurity
  @Profile({"!test"})
  @Order(1)
  // check against JWT and basic auth. You can also sign in using 'oauth2'
  public static class SecurityConfig {
    AccessService accessService;

    public SecurityConfig(AccessService accessService) {
      this.accessService = accessService;
    }

    @Bean
    @Order(1)
    protected SecurityFilterChain doPublic(HttpSecurity http) throws Exception {
      return http.securityMatcher(
              new AndRequestMatcher(
                  // used in config(2)
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/oauth2/**")),
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/login")),
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/login/**"))))
          .authorizeHttpRequests(
              requests ->
                  requests
                      .requestMatchers(
                          "/",
                          "/index.html",
                          "/armadillo-logo.png",
                          "favicon.ico",
                          "/assets/**",
                          "/v3/**",
                          "/swagger-ui/**",
                          "/ui/**",
                          "/swagger-ui.html",
                          "/basic-login")
                      .permitAll()
                      .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
                      .permitAll()
                      .requestMatchers(toAnyEndpoint())
                      .authenticated())
          .csrf()
          .disable()
          .cors()
          .and()
          .httpBasic(
              httpBasicConfigurer_ -> {
                httpBasicConfigurer_.realmName("Armadillo");
                httpBasicConfigurer_.authenticationEntryPoint(
                    new NoPopupBasicAuthenticationEntryPoint());
              })
          .logout()
          .logoutSuccessUrl("/")
          .and()
          .oauth2ResourceServer(
              oauth2 ->
                  oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())))
          .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
      JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
      jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
          new JwtRolesExtractor(accessService));
      return jwtAuthenticationConverter;
    }

    // oauth2login, if client is configured
    @Bean
    @Order(2)
    @ConditionalOnProperty("spring.security.oauth2.client.registration.molgenis.client-id")
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
      // use this if not authenticated and having oauth config
      return http.authorizeHttpRequests(
              requests ->
                  requests.requestMatchers("/oauth2/**", "/login", "/login/**").authenticated())
          .oauth2Login(
              oauth2Login ->
                  oauth2Login
                      .userInfoEndpoint(
                          userInfoEndpoint ->
                              userInfoEndpoint.userAuthoritiesMapper(this.userAuthoritiesMapper()))
                      .defaultSuccessUrl("/", true))
          .build();
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
      return authorities -> {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        authorities.forEach(
            authority -> {
              if (authority instanceof OAuth2UserAuthority) {
                OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                final Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
                mappedAuthorities.addAll(
                    runAsSystem(
                        () ->
                            accessService.getAuthoritiesForEmail(
                                (String) userAttributes.get("email"), userAttributes)));
              }
            });

        return mappedAuthorities;
      };
    }
  }

  /** Allow CORS requests, needed for swagger UI to work, if the development profile is active. */
  @Profile("development")
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    return request -> ALLOW_CORS;
  }

  /** Allow URL encoded slashes. Needed for the Storage API's object endpoints. */
  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }
}
