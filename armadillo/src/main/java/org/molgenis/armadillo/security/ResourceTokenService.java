package org.molgenis.armadillo.security;

import java.security.KeyPair;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class ResourceTokenService {

  private static final String INTERNAL_ISSUER = "armadillo-internal";
  private static final long TOKEN_VALIDITY_SECONDS = 300;
  private static final String RESOURCE_PROJECT_CLAIM = "resource_project";
  private static final String RESOURCE_OBJECT_CLAIM = "resource_object";

  private final JwtEncoder jwtEncoder;

  public ResourceTokenService(KeyPair resourceTokenKeyPair) {
    this.jwtEncoder =
        new NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(
                    new com.nimbusds.jose.jwk.RSAKey.Builder(
                            (java.security.interfaces.RSAPublicKey) resourceTokenKeyPair.getPublic())
                        .privateKey(
                            (java.security.interfaces.RSAPrivateKey)
                                resourceTokenKeyPair.getPrivate())
                        .build())));
  }

  public JwtAuthenticationToken generateResourceToken(String email, String project, String objectName) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(INTERNAL_ISSUER)
            .subject(email)
            .claim("email", email)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(TOKEN_VALIDITY_SECONDS))
            .claim(RESOURCE_PROJECT_CLAIM, project)
            .claim(RESOURCE_OBJECT_CLAIM, objectName)
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    return new JwtAuthenticationToken(jwt);
  }

}
