package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.armadillo.metadata.AuthLoader;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.security.OidcConfig;
import org.molgenis.armadillo.security.TestableClientRegistrationRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ManagementServiceTest.TestConfig.class)
class ManagementServiceTest {

  // Minimal Spring context — only needed to activate @PreAuthorize and @WithMockUser
  @Configuration
  @EnableMethodSecurity
  static class TestConfig {}

  private AuthLoader authLoader;
  private TestableClientRegistrationRepository registrationRepository;
  private ManagementService service;

  @BeforeEach
  void setUp() {
    authLoader = mock(AuthLoader.class);
    registrationRepository = new TestableClientRegistrationRepository();
    when(authLoader.load()).thenReturn(OidcDetails.create());
    service = new ManagementService(authLoader, registrationRepository, null, null, null);
  }

  // -------------------------------------------------------------------------
  // reloadOidcRegistration()
  // -------------------------------------------------------------------------

  @Test
  void reloadOidcRegistration_clearsRepository_whenAllFieldsNull() {
    assertThat(registrationRepository.isConfigured()).isFalse();
  }

  @Test
  void reloadOidcRegistration_loadsRepository_whenConfigIsComplete() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");
    service.reloadOidcRegistration();

    assertThat(registrationRepository.isConfigured()).isTrue();
  }

  @Test
  void reloadOidcRegistration_clearsRepository_whenIssuerUriMissing() {
    service = serviceWith(null, "secret", "client-id");
    service.reloadOidcRegistration();

    assertThat(registrationRepository.isConfigured()).isFalse();
  }

  @Test
  void reloadOidcRegistration_clearsRepository_whenClientIdMissing() {
    service = serviceWith("https://issuer.example.com", "secret", null);
    service.reloadOidcRegistration();

    assertThat(registrationRepository.isConfigured()).isFalse();
  }

  @Test
  void reloadOidcRegistration_clearsRepository_whenClientSecretMissing() {
    service = serviceWith("https://issuer.example.com", null, "client-id");
    service.reloadOidcRegistration();

    assertThat(registrationRepository.isConfigured()).isFalse();
  }

  // -------------------------------------------------------------------------
  // getOidcConfig()
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "SU")
  void getOidcConfig_returnsCurrentValues() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");

    OidcConfig config = service.getOidcConfig();

    assertThat(config.issuerUri()).isEqualTo("https://issuer.example.com");
    assertThat(config.clientId()).isEqualTo("client-id");
    assertThat(config.clientSecret()).isEqualTo("secret");
  }

  // -------------------------------------------------------------------------
  // saveNewOidcConfig()
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "SU")
  void saveNewOidcConfig_persistsSettings() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");
    service.saveNewOidcConfig("https://new-issuer.example.com", "new-client", "new-secret");

    verify(authLoader)
        .save(OidcDetails.create("https://new-issuer.example.com", "new-client", "new-secret"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void saveNewOidcConfig_reloadsRepository_withNewClientId() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");
    service.saveNewOidcConfig("https://new-issuer.example.com", "new-client", "new-secret");

    assertThat(registrationRepository.isConfigured()).isTrue();
    assertThat(registrationRepository.findByRegistrationId("molgenis").getClientId())
        .isEqualTo("new-client");
  }

  @Test
  @WithMockUser(roles = "SU")
  void saveNewOidcConfig_clearsRepository_whenFieldsAreNull() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");
    service.saveNewOidcConfig(null, null, null);

    assertThat(registrationRepository.isConfigured()).isFalse();
  }

  // -------------------------------------------------------------------------
  // bootstrap() — persisted config takes priority over application properties
  // -------------------------------------------------------------------------

  @Test
  void bootstrap_usesPersistedConfig_overDefaultProperties() {
    when(authLoader.load())
        .thenReturn(
            OidcDetails.create(
                "https://persisted.example.com", "persisted-client", "persisted-secret"));

    service = serviceWith("https://default.example.com", "default-secret", "default-client");

    OidcConfig config = runAsSystem(service::getOidcConfig);
    assertThat(config.issuerUri()).isEqualTo("https://persisted.example.com");
    assertThat(config.clientId()).isEqualTo("persisted-client");
  }

  @Test
  void bootstrap_usesDefaultProperties_whenPersistedConfigIsEmpty() {
    when(authLoader.load()).thenReturn(OidcDetails.create());

    service = serviceWith("https://default.example.com", "default-secret", "default-client");

    OidcConfig config = runAsSystem(service::getOidcConfig);
    assertThat(config.issuerUri()).isEqualTo("https://default.example.com");
    assertThat(config.clientId()).isEqualTo("default-client");
  }

  // -------------------------------------------------------------------------
  // save() / initialize()
  // -------------------------------------------------------------------------

  @Test
  void save_delegatesToLoader() {
    service = serviceWith("https://issuer.example.com", "secret", "client-id");
    service.save();

    verify(authLoader, times(3)).save(any());
  }

  @Test
  void initialize_loadsFromAuthLoader() {
    service.initialize();

    // loader.load() called once in constructor + once in explicit initialize()
    verify(authLoader, times(2)).load();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private ManagementService serviceWith(String issuerUri, String clientSecret, String clientId) {
    return new ManagementService(
        authLoader, registrationRepository, issuerUri, clientSecret, clientId);
  }

  /** Sets a system-level security context so @PreAuthorize-protected methods can be called. */
  private static <T> T runAsSystem(java.util.concurrent.Callable<T> action) {
    try {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "system", null, List.of(new SimpleGrantedAuthority("ROLE_SU"))));
      return action.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
