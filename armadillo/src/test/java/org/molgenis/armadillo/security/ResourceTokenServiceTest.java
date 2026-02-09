package org.molgenis.armadillo.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ResourceTokenServiceTest {

  private ResourceTokenService resourceTokenService;
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    resourceTokenService = new ResourceTokenService(300);

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

    assertThat(jwt.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
    assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void generateResourceToken_withJwtAuthenticationToken_usesEmailClaim() {
    Instant now = Instant.now();
    Jwt incomingJwt =
        Jwt.withTokenValue("dummy")
            .header("alg", "none")
            .claim("email", "jwt-user@example.org")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
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
