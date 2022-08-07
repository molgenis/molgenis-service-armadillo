package org.molgenis.armadillo.security;

import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.settings.ArmadilloSettingsService;
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
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Profile("!test")
@Import(UserDetailsServiceAutoConfiguration.class)
@EnableGlobalMethodSecurity
@EnableWebSecurity(debug = true)
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
  public static class JwtConfig extends WebSecurityConfigurerAdapter {
    ArmadilloSettingsService armadilloSettingsService;

    public JwtConfig(ArmadilloSettingsService armadilloSettingsService) {
      this.armadilloSettingsService = armadilloSettingsService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.requestMatcher(
              new AndRequestMatcher(
                  // used in config(2)
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/oauth2/**")),
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/login")),
                  new NegatedRequestMatcher(new AntPathRequestMatcher("/login/**"))))
          .authorizeRequests()
          .antMatchers("/", "/v3/**", "/swagger-ui/**", "/ui/**", "/swagger-ui.html")
          .permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
          .permitAll()
          .requestMatchers(toAnyEndpoint())
          .authenticated()
          .and()
          .csrf()
          .disable()
          .cors()
          .and()
          .httpBasic()
          .realmName("Armadillo")
          .and()
          .logout()
          .logoutSuccessUrl("/")
          .and()
          .oauth2ResourceServer(
              oauth2 ->
                  oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
      JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
      jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
          new JwtRolesExtractor(armadilloSettingsService));
      return jwtAuthenticationConverter;
    }
  }

  @Configuration
  @EnableWebSecurity
  @Order(2)
  @ConditionalOnProperty("spring.security.oauth2.client.registration.molgenis.client-id")
  @Profile({"!test"})
  // otherwise we gonna offer sign in
  public static class Oauth2LoginConfig extends WebSecurityConfigurerAdapter {
    ArmadilloSettingsService armadilloSettingsService;

    public Oauth2LoginConfig(ArmadilloSettingsService armadilloSettingsService) {
      this.armadilloSettingsService = armadilloSettingsService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      // use this if not authenticated and having oauth config
      http.authorizeRequests()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2Login(
              oauth2Login ->
                  oauth2Login
                      .userInfoEndpoint(
                          userInfoEndpoint ->
                              userInfoEndpoint.userAuthoritiesMapper(this.userAuthoritiesMapper()))
                      .defaultSuccessUrl("/swagger-ui/index.html", true));
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
                      armadilloSettingsService, (String) userAttributes.get("email")));
            });

        return mappedAuthorities;
      };
    }
  }

  @Profile("development")
  @Bean
  // Allow CORS requests, needed for swagger UI to work, if the development profile is active.
  CorsConfigurationSource corsConfigurationSource() {
    return request -> ALLOW_CORS;
  }

  public static Collection<SimpleGrantedAuthority> getAuthoritiesForEmail(
      ArmadilloSettingsService armadilloSettingsService, String email) {
    return armadilloSettingsService.getPermissionsForEmail(email).stream()
        .map(
            project ->
                "administrators".equals(project) ? "ROLE_SU" : "ROLE_" + project + "_RESEARCHER")
        .map(SimpleGrantedAuthority::new)
        .toList();
  }
}
