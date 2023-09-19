package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Collection;
import org.molgenis.armadillo.metadata.AccessService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/** Extracts roles from JWT */
@Service
public class JwtRolesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {
  private final AccessService accessService;

  public JwtRolesExtractor(AccessService accessService) {
    this.accessService = accessService;
  }

  public Collection<GrantedAuthority> convert(Jwt jwt) {
    return runAsSystem(
        () -> accessService.getAuthoritiesForEmail(jwt.getClaimAsString("email"), jwt.getClaims()));
  }
}
