package org.molgenis.armadillo.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ResourceTokenServiceTest {

  private static final long TOKEN_VALIDITY_SECONDS = 300;
  private static final Instant FIXED_NOW = Instant.parse("2026-06-18T10:00:00Z");

  private ResourceTokenService resourceTokenService;
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    resourceTokenService = new ResourceTokenService(TOKEN_VALIDITY_SECONDS);
    resourceTokenService.setClock(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

    RSAPublicKey publicKey = resourceTokenService.getPublicKey();
    jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
  }

  @Test
  void getPublicKey_returnsNonNullPublicKey() {
    RSAPublicKey publicKey = resourceTokenService.getPublicKey();

    assertThat(publicKey).isNotNull();
    assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  void generateResourceToken_withPlainPrincipal_setsExpectedClaims() {
    Principal principal = () -> "user@example.org";

    JwtAuthenticationToken token =
        resourceTokenService.generateResourceToken(principal, "test-project", "test-object");

    Jwt jwt = jwtDecoder.decode(token.getToken().getTokenValue());

    assertThat(jwt.getIssuer().getHost()).isEqualTo("armadillo-internal");
    assertThat(jwt.getSubject()).isEqualTo("user@example.org");

    assertThat(jwt.getClaimAsString("email")).isEqualTo("user@example.org");
    assertThat(jwt.getClaimAsString("resource_project")).isEqualTo("test-project");
    assertThat(jwt.getClaimAsString("resource_object")).isEqualTo("test-object");

    assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_NOW);
    assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_NOW.plusSeconds(TOKEN_VALIDITY_SECONDS));
  }

  @Test
  void generateResourceToken_withJwtAuthenticationToken_usesEmailClaim() {
    Jwt incomingJwt =
        Jwt.withTokenValue("dummy")
            .header("alg", "none")
            .claim("email", "jwt-user@example.org")
            .issuedAt(FIXED_NOW)
            .expiresAt(FIXED_NOW.plusSeconds(TOKEN_VALIDITY_SECONDS))
            .build();

    JwtAuthenticationToken principal = new JwtAuthenticationToken(incomingJwt);

    JwtAuthenticationToken resourceToken =
        resourceTokenService.generateResourceToken(principal, "project-a", "object-b");

    Jwt decodedJwt = jwtDecoder.decode(resourceToken.getToken().getTokenValue());

    assertThat(decodedJwt.getSubject()).isEqualTo("jwt-user@example.org");
    assertThat(decodedJwt.getClaimAsString("email")).isEqualTo("jwt-user@example.org");
  }

  @Test
  void generateResourceToken_createsValidSignedJwt() {
    Principal principal = () -> "signed@example.org";

    JwtAuthenticationToken token =
        resourceTokenService.generateResourceToken(principal, "secure-project", "secure-object");

    // Will throw if signature or structure is invalid
    Jwt decoded = jwtDecoder.decode(token.getToken().getTokenValue());

    assertThat(decoded.getHeaders()).containsEntry("alg", "RS256");
  }
}
