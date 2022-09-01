package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Collection;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/** Extracts roles from JWT */
@Service
@Profile({"armadillo"})
public class JwtRolesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {
  private final ArmadilloMetadataService armadilloMetadataService;

  public JwtRolesExtractor(ArmadilloMetadataService armadilloMetadataService) {
    this.armadilloMetadataService = armadilloMetadataService;
  }

  public Collection<GrantedAuthority> convert(Jwt jwt) {
    return runAsSystem(
        () ->
            armadilloMetadataService.getAuthoritiesForEmail(
                jwt.getClaimAsString("email"), jwt.getClaims()));
  }
}
