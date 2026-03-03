package org.molgenis.armadillo.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class ResourceTokenService {

  public static final String INTERNAL_ISSUER = "http://armadillo-internal";
  private final long tokenValiditySeconds;

  private static final String RESOURCE_PROJECT_CLAIM = "resource_project";
  private static final String RESOURCE_OBJECT_CLAIM = "resource_object";

  private final JwtEncoder jwtEncoder;
  private final RSAPublicKey publicKey;

  public ResourceTokenService(
      @Value("${storage.resource-token-timeout:300}") long tokenValiditySeconds) {
    try {
      this.tokenValiditySeconds = tokenValiditySeconds;
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      this.publicKey = (RSAPublicKey) keyPair.getPublic();
      this.jwtEncoder =
          new NimbusJwtEncoder(
              new ImmutableJWKSet<>(
                  new JWKSet(
                      new RSAKey.Builder(publicKey)
                          .privateKey((RSAPrivateKey) keyPair.getPrivate())
                          .build())));
    } catch (Exception e) {
      throw new SecurityException("Failed to initialize RSA key pair", e);
    }
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  public JwtAuthenticationToken generateResourceToken(
      Principal principal, String project, String objectName) {
    String email =
        principal instanceof JwtAuthenticationToken token
            ? token.getToken().getClaimAsString("email")
            : principal.getName();

    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(INTERNAL_ISSUER)
            .subject(email)
            .claim("email", email)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(tokenValiditySeconds))
            .claim(RESOURCE_PROJECT_CLAIM, project)
            .claim(RESOURCE_OBJECT_CLAIM, objectName)
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    return new JwtAuthenticationToken(jwt);
  }
}
