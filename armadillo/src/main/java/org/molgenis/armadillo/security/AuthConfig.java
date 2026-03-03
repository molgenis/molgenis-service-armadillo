package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.*;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.service.ManagementService;
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
// Three auth mechanisms are supported: JWT (order 1, most dominant), basic auth, and OAuth2 login
// (least dominant). In the 'test' profile none of these are enabled.
public class AuthConfig {

  private static final CorsConfiguration ALLOW_CORS =
      new CorsConfiguration().applyPermitDefaultValues();

  private static final String[] PUBLIC_PATHS = {
    "/",
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
    "/ds-profiles/status",
    "/swagger-ui.html"
  };

  // Whether OIDC login is available depends on a client ID being configured
  private final boolean oidcEnabled;
  private final AccessService accessService;

  @Value("${armadillo.api-key:#{null}}")
  private String apiKey;

  @Value("${spring.security.user.name}")
  private String userName;

  @Value("${spring.security.user.password}")
  private String userPassword;

  public AuthConfig(AccessService accessService, ManagementService managementService) {
    this.oidcEnabled = managementService.getClientId() != null;
    this.accessService = accessService;
  }

  // -------------------------------------------------------------------------
  // Security filter chain
  // -------------------------------------------------------------------------

  @Order(1)
  @Bean
  protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    configurePublicAndAuthenticatedPaths(http);
    configureBasicAuth(http);
    if (oidcEnabled) {
      configureOAuth2AndJwt(http);
    }
    http.csrf(AbstractHttpConfigurer::disable);
    http.cors(Customizer.withDefaults());
    http.addFilterAfter(apiKeyFilter(), BasicAuthenticationFilter.class);
    return http.build();
  }

  private void configurePublicAndAuthenticatedPaths(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        requests ->
            requests
                .requestMatchers(PUBLIC_PATHS)
                .permitAll()
                .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
                .permitAll()
                .anyRequest()
                .authenticated());
  }

  private void configureBasicAuth(HttpSecurity http) throws Exception {
    http.httpBasic(
        configurer ->
            configurer
                // Store basic auth in the session so it survives alongside stateless OAuth2.
                // See:
                // https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html#storing-stateless-authentication-in-the-session
                .withObjectPostProcessor(saveBasicAuthToSession())
                .realmName("Armadillo")
                .authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint()));
  }

  private void configureOAuth2AndJwt(HttpSecurity http) throws Exception {
    http.oauth2Login(
        configurer ->
            configurer
                .userInfoEndpoint(
                    endpoint -> endpoint.userAuthoritiesMapper(userAuthoritiesMapper()))
                .defaultSuccessUrl("/", true));
    http.oauth2ResourceServer(
        configurer -> configurer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())));
  }

  // -------------------------------------------------------------------------
  // Auth helpers
  // -------------------------------------------------------------------------

  private AuthenticationFilter apiKeyFilter() {
    AuthenticationFilter filter = new AuthenticationFilter();
    filter.setAuthToken(apiKey);
    return filter;
  }

  private ObjectPostProcessor<BasicAuthenticationFilter> saveBasicAuthToSession() {
    return new ObjectPostProcessor<>() {
      @Override
      public <O extends BasicAuthenticationFilter> O postProcess(O filter) {
        filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        return filter;
      }
    };
  }

  private Converter<Jwt, AbstractAuthenticationToken> jwtConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtRolesExtractor(accessService));
    return converter;
  }

  private GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return authorities -> {
      Set<GrantedAuthority> mapped = new HashSet<>();
      authorities.forEach(
          authority -> {
            if (authority instanceof OAuth2UserAuthority oAuth2UserAuthority) {
              Map<String, Object> attributes = oAuth2UserAuthority.getAttributes();
              mapped.addAll(
                  runAsSystem(
                      () ->
                          accessService.getAuthoritiesForEmail(
                              (String) attributes.get("email"), attributes)));
            }
          });
      return mapped;
    };
  }

  // -------------------------------------------------------------------------
  // Infrastructure beans
  // -------------------------------------------------------------------------

  /** Permit CORS requests — required for Swagger UI when the dev profile is active. */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    return request -> ALLOW_CORS;
  }

  /** Allow URL-encoded slashes, required by the Storage API's object endpoints. */
  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

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
