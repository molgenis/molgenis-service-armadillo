package org.molgenis.armadillo.security;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

/**
 * A {@link ClientRegistrationRepository} whose registration can be replaced at runtime without
 * restarting the application. When no OIDC config is loaded, {@link #findByRegistrationId} returns
 * {@code null}, causing Spring Security to skip OAuth2 login and fall back to basic auth.
 */
@Component
public class DynamicClientRegistrationRepository
    implements ClientRegistrationRepository, Iterable<ClientRegistration> {

  public static final String REGISTRATION_ID = "molgenis";

  private final AtomicReference<ClientRegistration> registration = new AtomicReference<>();

  /** Returns the active registration, or {@code null} if none is loaded. */
  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    if (!REGISTRATION_ID.equals(registrationId)) {
      return null;
    }
    return registration.get();
  }

  /** Required by Spring's OAuth2 login auto-configuration to iterate registrations. */
  @Override
  public Iterator<ClientRegistration> iterator() {
    ClientRegistration current = registration.get();
    return (current != null ? List.of(current) : List.<ClientRegistration>of()).iterator();
  }

  /** Returns true if an OIDC config is currently loaded. */
  public boolean isConfigured() {
    return registration.get() != null;
  }

  /**
   * Replaces the active registration by performing OIDC discovery against the issuer URI (fetches
   * /.well-known/openid-configuration). Throws if the issuer is unreachable or returns an invalid
   * document — the caller is responsible for handling this.
   */
  public void load(OidcConfig config) {
    registration.set(discoverRegistration(config));
  }

  /** Clears the active registration, effectively disabling OAuth2 login. */
  public void clear() {
    registration.set(null);
  }

  /**
   * Uses OIDC discovery to resolve all endpoint URIs from the issuer's
   * /.well-known/openid-configuration, then overlays the client credentials. Throws {@link
   * IllegalStateException} if discovery fails.
   */
  private static ClientRegistration discoverRegistration(OidcConfig config) {
    return ClientRegistrations.fromIssuerLocation(config.issuerUri())
        .registrationId(REGISTRATION_ID)
        .clientId(config.clientId())
        .clientSecret(config.clientSecret())
        .scope("openid", "email", "profile")
        .build();
  }
}
