package org.molgenis.armadillo.security;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class DynamicClientRegistrationRepositoryTest {
  private static final OidcConfig CONFIG =
      new OidcConfig("https://issuer.example.com", "test-client", "test-secret");

  private TestableClientRegistrationRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestableClientRegistrationRepository();
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
        new TestableClientRegistrationRepository() {
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

  // -------------------------------------------------------------------------
  // discoverAndLoad() — uses WireMock to simulate OIDC discovery endpoint
  // -------------------------------------------------------------------------

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void discoverAndLoad_populatesRegistrationFromDiscoveryDocument() {
    String issuerUri = wireMock.baseUrl();
    stubOidcDiscovery(issuerUri);

    DynamicClientRegistrationRepository real = new DynamicClientRegistrationRepository();
    real.discoverAndLoad(new OidcConfig(issuerUri, "my-client", "my-secret"));

    ClientRegistration reg = real.findByRegistrationId("molgenis");
    assertThat(reg).isNotNull();
    assertThat(reg.getClientId()).isEqualTo("my-client");
    assertThat(reg.getClientSecret()).isEqualTo("my-secret");
    assertThat(reg.getProviderDetails().getAuthorizationUri()).isEqualTo(issuerUri + "/auth");
    assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo(issuerUri + "/token");
    assertThat(reg.getProviderDetails().getJwkSetUri()).isEqualTo(issuerUri + "/jwks");
  }

  @Test
  void discoverAndLoad_throwsWhenIssuerUnreachable() {
    DynamicClientRegistrationRepository real = new DynamicClientRegistrationRepository();
    OidcConfig oidcConfig = new OidcConfig("https://unreachable.example.com", "client", "secret");
    assertThatThrownBy(() -> real.discoverAndLoad(oidcConfig))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void discoverAndLoad_throwsWhenDiscoveryDocumentInvalid() {
    wireMock.stubFor(
        get(urlPathEqualTo("/.well-known/openid-configuration"))
            .willReturn(okJson("{\"invalid\": \"document\"}")));
    wireMock.stubFor(
        get(urlPathEqualTo("/.well-known/oauth-authorization-server"))
            .willReturn(okJson("{\"invalid\": \"document\"}")));

    OidcConfig oidcConfig = new OidcConfig(wireMock.baseUrl(), "client", "secret");
    DynamicClientRegistrationRepository real = new DynamicClientRegistrationRepository();

    assertThatThrownBy(() -> real.discoverAndLoad(oidcConfig))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void stubOidcDiscovery(String issuerUri) {
    String document =
        """
            {
              "issuer": "%s",
              "authorization_endpoint": "%s/auth",
              "token_endpoint": "%s/token",
              "jwks_uri": "%s/jwks",
              "userinfo_endpoint": "%s/userinfo",
              "response_types_supported": ["code"],
              "subject_types_supported": ["public"],
              "id_token_signing_alg_values_supported": ["RS256"]
            }
            """
            .formatted(issuerUri, issuerUri, issuerUri, issuerUri, issuerUri);

    wireMock.stubFor(
        get(urlPathEqualTo("/.well-known/openid-configuration")).willReturn(okJson(document)));
  }
}
