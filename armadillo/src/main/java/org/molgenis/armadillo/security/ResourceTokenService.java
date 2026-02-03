package org.molgenis.armadillo.security;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
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

  public String generateResourceToken(String email, String project, String objectName) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(INTERNAL_ISSUER)
            .subject(email)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(TOKEN_VALIDITY_SECONDS))
            .claim(RESOURCE_PROJECT_CLAIM, project)
            .claim(RESOURCE_OBJECT_CLAIM, objectName)
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public boolean isInternalToken(Jwt jwt) {
    return INTERNAL_ISSUER.equals(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
  }

  public Collection<GrantedAuthority> extractResourceRole(Jwt jwt) {
    String project = jwt.getClaimAsString(RESOURCE_PROJECT_CLAIM);
    String objectName = jwt.getClaimAsString(RESOURCE_OBJECT_CLAIM);

    if (project == null || objectName == null) {
      return Collections.emptyList();
    }

    String roleName = "ROLE_RESOURCE_VIEW_" + normalizeResourceName(project, objectName);
    return Collections.singleton(new SimpleGrantedAuthority(roleName));
  }

  public String normalizeResourceName(String project, String objectName) {
    String combined = project + "_" + objectName;
    return combined.toUpperCase().replaceAll("[^A-Z0-9]", "_");
  }
}
