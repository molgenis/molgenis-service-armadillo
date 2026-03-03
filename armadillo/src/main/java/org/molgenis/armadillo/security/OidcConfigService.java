package org.molgenis.armadillo.security;

import jakarta.annotation.PostConstruct;
import org.molgenis.armadillo.service.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages the lifecycle of the OIDC configuration at runtime. Call {@link #reload()} from an admin
 * endpoint to pick up a new config without restarting the application.
 */
@Service
public class OidcConfigService {

  private static final Logger logger = LoggerFactory.getLogger(OidcConfigService.class);

  private final ManagementService managementService;
  private final DynamicClientRegistrationRepository registrationRepository;

  public OidcConfigService(
      ManagementService managementService,
      DynamicClientRegistrationRepository registrationRepository) {
    this.managementService = managementService;
    this.registrationRepository = registrationRepository;
  }

  /**
   * Loads the OIDC config from {@link ManagementService} into the registration repository. If no
   * config is available, OAuth2 login is disabled and basic auth remains active. Called on startup
   * and whenever an admin triggers a reload.
   */
  @PostConstruct
  public void reload() {
    OidcConfig config = managementService.getOidcConfig();
    if (config == null) {
      logger.info("No OIDC config available — OAuth2 login disabled, falling back to basic auth");
      registrationRepository.clear();
    } else {
      logger.info("Loading OIDC config for issuer: {}", config.issuerUri());
      registrationRepository.load(config);
    }
  }
}
