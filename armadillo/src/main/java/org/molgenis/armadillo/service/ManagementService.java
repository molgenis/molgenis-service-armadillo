package org.molgenis.armadillo.service;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Map;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.metadata.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ManagementService {
  private final AuthLoader loader;
  private AuthMetadata settings;
  private String clientId;
  private String clientSecret;
  static String authServerUri;

  public ManagementService(
      AuthLoader authLoader,
      @Value("${spring.security.oauth2.client.provider.molgenis.issuer-uri:#{null}}")
          String defaultAuthServerUri,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
          String defaultClientSecret,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
          String defaultClientId) {
    this.loader = requireNonNull(authLoader);
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

  @PreAuthorize("hasRole('ROLE_SU')")
  public Map<String, String> getClient() {
    return Map.of(
        "client-id", clientId,
        "client-secret", clientSecret);
  }

  public String getClientId() {
    return clientId;
  }

  public Boolean getOidcPermissionsEnabled() {
    return clientId != null && clientSecret != null && authServerUri != null;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void restartApplication() {
    ArmadilloServiceApplication.restart();
  }

  public void save() {
    loader.save(settings);
  }

  private void bootstrap() {
    authServerUri =
        settings.getAuthServerUri().isEmpty() ? authServerUri : settings.getAuthServerUri();
    clientId = settings.getAuthServerUri().isEmpty() ? clientId : settings.getClientId();
    clientSecret =
        settings.getAuthServerUri().isEmpty() ? clientSecret : settings.getClientSecret();
    settings = AuthMetadata.create(clientId, clientSecret, authServerUri);
    save();
  }
}
