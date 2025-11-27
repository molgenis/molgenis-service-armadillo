package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.*;
import org.molgenis.armadillo.metadata.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
// we have three configs that enable jwt, formLogin and oauth2Login respectively.
// they are ordered, so jwt config is most dominant and oauth2Login least dominant
// in 'test' container they are not enabled
public class AuthConfig {
  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();
  private final AccessService accessService;

  public AuthConfig(AccessService accessService) {
    this.accessService = accessService;
  }

  @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
  private String oidcClientId;

  @Bean
  @Order(1)
  protected SecurityFilterChain oauthAndBasic(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        requests ->
            requests
                .requestMatchers(
                    "/",
                    "/_docs/**",
                    "/info",
                    "/index.html",
                    "/logout",
                    "/basic-login",
                    "/my/**",
                    "/armadillo-logo.png",
                    "/favicon.ico",
                    "/assets/**",
                    "/v3/**",
                    "/swagger-ui/**",
                    "/ui/**",
                    "/containers/status",
                    "/actuator/prometheus",
                    "/swagger-ui.html")
                .permitAll()
                .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
                .permitAll()
                .anyRequest()
                .authenticated());
    http.csrf(AbstractHttpConfigurer::disable);
    http.cors(Customizer.withDefaults());
    http.httpBasic(
        httpBasicConfigurer ->
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
                .authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint()));
    if (oidcClientId != null) {
      http.oauth2Login(
          oauth2Login ->
              oauth2Login
                  .userInfoEndpoint(
                      userInfoEndpoint ->
                          userInfoEndpoint.userAuthoritiesMapper(this.userAuthoritiesMapper()))
                  .defaultSuccessUrl("/", true));
      http.oauth2ResourceServer(
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
            if (authority instanceof OAuth2UserAuthority oAuth2UserAuthority) {
              final Map<String, Object> userAttributes = oAuth2UserAuthority.getAttributes();
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

  /** Allow CORS requests, needed for swagger UI to work, if the development container is active. */
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

  // we do this by hand because auto configure is disabled when oauth2 is enabled
  @Value("${spring.security.user.name}")
  private String userName;

  @Value("${spring.security.user.password}")
  private String userPassword;

  @Bean
  public UserDetailsService userDetailsService() {
    Objects.requireNonNull(userName, "spring.security.user.name is null");
    Objects.requireNonNull(userPassword, "spring.security.user.password is null");
    PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    return new InMemoryUserDetailsManager(
        User.builder()
            .username(userName)
            .password(encoder.encode(userPassword))
            .roles("SU")
            .build());
  }
}
