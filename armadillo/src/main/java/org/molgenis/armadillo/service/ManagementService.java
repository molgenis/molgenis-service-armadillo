package org.molgenis.armadillo.service;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Map;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {
  public static final String AUDIT_FILE = "AUDIT_FILE";
  public static final String LOG_FILE = "LOG_FILE";

  private final ArmadilloStorageService storage;
  private final AuthLoader loader;
  private AuthMetadata settings;
  private String clientId;
  private String clientSecret;
  static String authServerUri;

  public ManagementService(
      ArmadilloStorageService armadilloStorageService,
      AuthLoader authLoader,
      @Value("${spring.security.oauth2.client.provider.molgenis.issuer-uri:#{null}}")
          String defaultAuthServerUri,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
          String defaultClientSecret,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
          String defaultClientId) {
    this.loader = requireNonNull(authLoader);
    this.storage = requireNonNull(armadilloStorageService);
    authServerUri = defaultAuthServerUri;
    clientId = defaultClientId;
    clientSecret = defaultClientSecret;
    runAsSystem(this::initialize);
  }

  /**
   * Initialization separated from constructor so that it can be called in WebMvc tests
   * <strong>after</strong> mocks have been initialized.
   */
  public void initialize() {
    settings = loader.load();
    bootstrap();
  }

  public Map<String, String> getClient() {
    return Map.of(
        "client-id", clientId,
        "client-secret", clientSecret);
  }

  public void restartApplication() {
    ArmadilloServiceApplication.restart();
  }

  public void save() {
    loader.save(settings);
  }

  private void bootstrap() {
    String authServerUriConfig =
        settings.getAuthServerUri().isEmpty() ? authServerUri : settings.getAuthServerUri();
    String clientIdConfig =
        settings.getAuthServerUri().isEmpty() ? clientId : settings.getClientId();
    String clientSecretConfig =
        settings.getAuthServerUri().isEmpty() ? clientSecret : settings.getClientSecret();
    settings = AuthMetadata.create(clientIdConfig, clientSecretConfig, authServerUriConfig);
    save();
  }
}
