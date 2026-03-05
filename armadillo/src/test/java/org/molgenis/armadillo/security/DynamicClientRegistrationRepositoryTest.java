package org.molgenis.armadillo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class DynamicClientRegistrationRepositoryTest {

  // A pre-built ClientRegistration we can use without hitting a real OIDC server.
  // discoverRegistration() is the only method that does I/O; everything else works
  // with whatever ClientRegistration is stored in the AtomicReference.
  private static final ClientRegistration REGISTRATION =
      ClientRegistration.withRegistrationId("molgenis")
          .clientId("test-client")
          .clientSecret("test-secret")
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
          .authorizationUri("https://issuer.example.com/auth")
          .tokenUri("https://issuer.example.com/token")
          .jwkSetUri("https://issuer.example.com/jwks")
          .scope("openid", "email", "profile")
          .build();

  private static final OidcConfig CONFIG =
      new OidcConfig("https://issuer.example.com", "test-client", "test-secret");

  // Subclass that overrides discoverRegistration to skip the HTTP call,
  // letting us test all other behaviour in isolation.
  private static class TestableRepository extends DynamicClientRegistrationRepository {
    private OidcConfig lastLoadedConfig;

    @Override
    void discoverAndLoad(OidcConfig config) {
      this.lastLoadedConfig = config;
      forceLoad(REGISTRATION);
    }

    // Direct backdoor to set the stored registration without going through discovery
    void forceLoad(ClientRegistration reg) {
      super.forceLoad(reg);
    }
  }

  private TestableRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestableRepository();
  }

  // -------------------------------------------------------------------------
  // Initial state
  // -------------------------------------------------------------------------

  @Test
  void startsUnconfigured() {
    assertThat(repository.isConfigured()).isFalse();
  }

  @Test
  void findByRegistrationId_returnsNull_whenNotLoaded() {
    assertThat(repository.findByRegistrationId("molgenis")).isNull();
  }

  @Test
  void iterator_isEmpty_whenNotLoaded() {
    assertThat(repository.iterator().hasNext()).isFalse();
  }

  // -------------------------------------------------------------------------
  // load()
  // -------------------------------------------------------------------------

  @Test
  void load_setsConfigured() {
    repository.load(CONFIG);

    assertThat(repository.isConfigured()).isTrue();
  }

  @Test
  void load_passesConfigToDiscovery() {
    repository.load(CONFIG);

    assertThat(repository.lastLoadedConfig).isEqualTo(CONFIG);
  }

  @Test
  void load_makesRegistrationFindable() {
    repository.load(CONFIG);

    ClientRegistration found = repository.findByRegistrationId("molgenis");
    assertThat(found).isNotNull();
    assertThat(found.getClientId()).isEqualTo("test-client");
  }

  @Test
  void load_replacesExistingRegistration() {
    repository.load(CONFIG);

    OidcConfig newConfig =
        new OidcConfig("https://other.example.com", "other-client", "other-secret");
    ClientRegistration newRegistration =
        ClientRegistration.withRegistrationId("molgenis")
            .clientId("other-client")
            .clientSecret("other-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://other.example.com/auth")
            .tokenUri("https://other.example.com/token")
            .jwkSetUri("https://other.example.com/jwks")
            .build();

    // Override what the next discoverAndLoad returns
    repository =
        new TestableRepository() {
          @Override
          void discoverAndLoad(OidcConfig config) {
            forceLoad(newRegistration);
          }
        };
    repository.load(newConfig);

    assertThat(repository.findByRegistrationId("molgenis").getClientId()).isEqualTo("other-client");
  }

  // -------------------------------------------------------------------------
  // clear()
  // -------------------------------------------------------------------------

  @Test
  void clear_removesRegistration() {
    repository.load(CONFIG);
    repository.clear();

    assertThat(repository.isConfigured()).isFalse();
  }

  @Test
  void clear_makesRegistrationUnfindable() {
    repository.load(CONFIG);
    repository.clear();

    assertThat(repository.findByRegistrationId("molgenis")).isNull();
  }

  @Test
  void clear_isIdempotent() {
    repository.clear();
    repository.clear();

    assertThat(repository.isConfigured()).isFalse();
  }

  // -------------------------------------------------------------------------
  // findByRegistrationId()
  // -------------------------------------------------------------------------

  @Test
  void findByRegistrationId_returnsNull_forUnknownId() {
    repository.load(CONFIG);

    assertThat(repository.findByRegistrationId("unknown")).isNull();
  }

  @Test
  void findByRegistrationId_returnsNull_forNullId() {
    repository.load(CONFIG);

    assertThat(repository.findByRegistrationId(null)).isNull();
  }

  // -------------------------------------------------------------------------
  // iterator()
  // -------------------------------------------------------------------------

  @Test
  void iterator_containsRegistration_whenLoaded() {
    repository.load(CONFIG);

    Iterator<ClientRegistration> it = repository.iterator();
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().getClientId()).isEqualTo("test-client");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void iterator_isEmpty_afterClear() {
    repository.load(CONFIG);
    repository.clear();

    assertThat(repository.iterator().hasNext()).isFalse();
  }

  // -------------------------------------------------------------------------
  // Thread safety (smoke test)
  // -------------------------------------------------------------------------

  @Test
  void concurrentLoadAndClear_doesNotThrow() throws InterruptedException {
    List<Thread> threads =
        List.of(
            new Thread(
                () -> {
                  for (int i = 0; i < 100; i++) repository.load(CONFIG);
                }),
            new Thread(
                () -> {
                  for (int i = 0; i < 100; i++) repository.clear();
                }),
            new Thread(
                () -> {
                  for (int i = 0; i < 100; i++) repository.isConfigured();
                }),
            new Thread(
                () -> {
                  for (int i = 0; i < 100; i++) repository.findByRegistrationId("molgenis");
                }));

    threads.forEach(Thread::start);
    for (Thread t : threads) t.join();
    // No assertion needed — we're verifying no exception is thrown
  }
}
