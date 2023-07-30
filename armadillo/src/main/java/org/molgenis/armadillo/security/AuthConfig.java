package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.AccessService;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Import(UserDetailsServiceAutoConfiguration.class)
@EnableMethodSecurity
@EnableWebSecurity
@Configuration
// we have three configs that enable jwt, formLogin and oauth2Login respectively.
// they are ordered, so jwt config is most dominant and oauth2Login least dominant
// in 'test' profile they are not enabled
public class AuthConfig {
  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();
  private AccessService accessService;

  public AuthConfig(AccessService accessService) {
    this.accessService = accessService;
  }

  @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
  private String oidcClientId;

  @Bean
  @Order(1)
  protected SecurityFilterChain oauthAndBasic(HttpSecurity http) throws Exception {
    http =
        http.authorizeHttpRequests(
                requests ->
                    requests
                        .requestMatchers(
                            "/",
                            "/info",
                            "/index.html",
                            "/logout",
                            "/basic-login",
                            "/my/**",
                            "/armadillo-logo.png",
                            "favicon.ico",
                            "/assets/**",
                            "/v3/**",
                            "/swagger-ui/**",
                            "/ui/**",
                            "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(
                            EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
                        .permitAll()
                        .anyRequest()
                        .authenticated())
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .httpBasic(
                httpBasicConfigurer -> {
                  httpBasicConfigurer
                      .withObjectPostProcessor(
                          new ObjectPostProcessor<BasicAuthenticationFilter>() {
                            // save of basic auth in the session because oauth2 make it stateless
                            // https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html#storing-stateless-authentication-in-the-session
                            @Override
                            public <O extends BasicAuthenticationFilter> O postProcess(O filter) {
                              filter.setSecurityContextRepository(
                                  new HttpSessionSecurityContextRepository());
                              return filter;
                            }
                          })
                      .realmName("Armadillo")
                      .authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint());
                });
    if (oidcClientId != null) {
      http.oauth2Login(
              oauth2Login ->
                  oauth2Login
                      .userInfoEndpoint(
                          userInfoEndpoint ->
                              userInfoEndpoint.userAuthoritiesMapper(this.userAuthoritiesMapper()))
                      .defaultSuccessUrl("/", true))
          .oauth2ResourceServer(
              oauth2 ->
                  oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));
    }
    return http.build();
  }

  Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        new JwtRolesExtractor(accessService));
    return jwtAuthenticationConverter;
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
