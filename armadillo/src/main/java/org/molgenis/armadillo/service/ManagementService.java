package org.molgenis.armadillo.service;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.security.DynamicClientRegistrationRepository;
import org.molgenis.armadillo.security.OidcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ManagementService {

  private static final Logger logger = LoggerFactory.getLogger(ManagementService.class);

  private final AuthLoader loader;
  private final DynamicClientRegistrationRepository registrationRepository;
  private AuthMetadata settings;
  private String clientId;
  private String clientSecret;
  static String issuerUri;

  public ManagementService(
      AuthLoader authLoader,
      DynamicClientRegistrationRepository registrationRepository,
      @Value("${spring.security.oauth2.client.provider.molgenis.issuer-uri:#{null}}")
          String defaultAuthServerUri,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
          String defaultClientSecret,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
          String defaultClientId) {
    this.loader = requireNonNull(authLoader);
    this.registrationRepository = requireNonNull(registrationRepository);
    issuerUri = defaultAuthServerUri;
    clientId = defaultClientId;
    clientSecret = defaultClientSecret;
    runAsSystem(this::initialize);
  }

  /**
   * Initialization separated from constructor so that it can be called in WebMvc tests after mocks
   * have been initialized.
   */
  public void initialize() {
    settings = loader.load();
    bootstrap();
  }

  // -------------------------------------------------------------------------
  // OIDC registration lifecycle
  // -------------------------------------------------------------------------

  /**
   * Loads the current OIDC config into the registration repository, enabling OAuth2 login. If the
   * config is incomplete, OAuth2 login is disabled and basic auth remains active. Called
   * automatically on startup and can be triggered explicitly via the admin endpoint.
   */
  @PostConstruct
  public void reloadOidcRegistration() {
    OidcConfig config = runAsSystem(this::getOidcConfig);
    if (!isOidcConfigComplete(config)) {
      logger.info("No OIDC config available - OAuth2 login disabled, falling back to basic auth");
      registrationRepository.clear();
    } else {
      logger.info("Loading OIDC config for issuer: {}", config.issuerUri());
      registrationRepository.load(config);
    }
  }

  // -------------------------------------------------------------------------
  // OIDC config read/write
  // -------------------------------------------------------------------------

  @PreAuthorize("hasRole('ROLE_SU')")
  public OidcConfig getOidcConfig() {
    return new OidcConfig(issuerUri, clientId, clientSecret);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void saveNewOidcConfig(String newIssuerUri, String newClientId, String newClientSecret) {
    saveSettings(newIssuerUri, newClientId, newClientSecret);
    reloadOidcRegistration();
  }

  // -------------------------------------------------------------------------
  // Application management
  // -------------------------------------------------------------------------

  @PreAuthorize(" hasRole('ROLE_SU')")
  public Map<String, String> getClient() {
    return Map.of(
        "client-id", clientId,
        "client-secret", clientSecret);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void restartApplication() {
    ArmadilloServiceApplication.restart();
  }

  public void save() {
    loader.save(settings);
  }

  // -------------------------------------------------------------------------
  // Internals
  // -------------------------------------------------------------------------

  private void bootstrap() {
    String auth = settings.getIssuerUri().isEmpty() ? issuerUri : settings.getIssuerUri();
    String client = settings.getClientId().isEmpty() ? clientId : settings.getClientId();
    String secret =
        settings.getClientSecret().isEmpty() ? clientSecret : settings.getClientSecret();
    saveSettings(auth, client, secret);
  }

  private void saveSettings(String newIssuerUri, String newClientId, String newClientSecret) {
    issuerUri = newIssuerUri;
    this.clientId = newClientId;
    this.clientSecret = newClientSecret;
    settings = AuthMetadata.create(newClientId, newClientSecret, newIssuerUri);
    save();
  }

  /**
   * A config is only usable if all three fields are present - a partially configured OIDC setup
   * would fail at the auth server anyway.
   */
  private static boolean isOidcConfigComplete(OidcConfig config) {
    return config != null
        && config.issuerUri() != null
        && config.clientId() != null
        && config.clientSecret() != null;
  }
}
