package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
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
// Three auth mechanisms are supported: JWT (most dominant), basic auth, and OAuth2 login
// (least dominant). In the 'test' profile none of these are enabled.
// OAuth2/JWT are only active when an OIDC config has been loaded via OidcConfigService.reload().
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

  private final AccessService accessService;

  @Value("${armadillo.api-key:#{null}}")
  private String apiKey;

  @Value("${spring.security.user.name}")
  private String userName;

  @Value("${spring.security.user.password}")
  private String userPassword;

  public AuthConfig(AccessService accessService) {
    this.accessService = accessService;
  }

  // -------------------------------------------------------------------------
  // Beans
  // -------------------------------------------------------------------------

  /**
   * The registration repository is a singleton shared between {@link AuthConfig} and {@link
   * OidcConfigService}. OidcConfigService.reload() swaps its contents at runtime; Spring's OAuth2
   * login reads from it on every request.
   */
  @Bean
  public DynamicClientRegistrationRepository clientRegistrationRepository() {
    return new DynamicClientRegistrationRepository();
  }

  // -------------------------------------------------------------------------
  // Security filter chain
  // -------------------------------------------------------------------------

  @Order(1)
  @Bean
  protected SecurityFilterChain securityFilterChain(
      HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository)
      throws Exception {
    configurePublicAndAuthenticatedPaths(http);
    configureBasicAuth(http);
    configureOAuth2AndJwt(http, clientRegistrationRepository);
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

  /**
   * OAuth2 login and JWT are always wired, but the {@link DynamicClientRegistrationRepository}
   * returns null when no config is loaded — Spring Security then skips OAuth2 login and falls back
   * to basic auth transparently.
   *
   * <p>The authorization request resolver must be lazy. When you supply your own
   * ClientRegistrationRepository bean, Spring's OAuth2 auto-configuration backs off completely. An
   * eagerly constructed DefaultOAuth2AuthorizationRequestResolver would capture the repository
   * state at startup — before OidcConfigService.reload() runs — and never redirect correctly. The
   * lazy resolver re-evaluates the repository on every incoming request instead.
   */
  private void configureOAuth2AndJwt(
      HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository)
      throws Exception {
    http.oauth2Login(
        configurer ->
            configurer
                .clientRegistrationRepository(clientRegistrationRepository)
                .authorizationEndpoint(
                    endpoint ->
                        endpoint.authorizationRequestResolver(
                            lazyAuthorizationRequestResolver(clientRegistrationRepository)))
                .userInfoEndpoint(
                    endpoint -> endpoint.userAuthoritiesMapper(userAuthoritiesMapper()))
                .defaultSuccessUrl("/", true));
    http.oauth2ResourceServer(
        configurer -> configurer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())));
  }

  // -------------------------------------------------------------------------
  // Auth helpers
  // -------------------------------------------------------------------------

  /**
   * Returns a resolver that constructs a fresh {@link DefaultOAuth2AuthorizationRequestResolver} on
   * every request. This is necessary because the repository may be empty at filter chain build time
   * (before OidcConfigService.reload() is called) — an eagerly constructed resolver would capture a
   * null registration and never redirect to the auth server.
   */
  private OAuth2AuthorizationRequestResolver lazyAuthorizationRequestResolver(
      ClientRegistrationRepository repo) {
    return new OAuth2AuthorizationRequestResolver() {
      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return newResolver().resolve(request);
      }

      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return newResolver().resolve(request, registrationId);
      }

      private DefaultOAuth2AuthorizationRequestResolver newResolver() {
        return new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
      }
    };
  }

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
