package org.molgenis.armadillo.security;

import static java.util.Collections.emptyList;
import static org.molgenis.armadillo.security.AuthConfig.getAuthoritiesForEmail;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/** Extracts roles from JWT */
@Service
public class JwtRolesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {
  private AccessStorageService accessStorageService;

  public JwtRolesExtractor(AccessStorageService accessStorageService) {
    this.accessStorageService = accessStorageService;
  }

  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<GrantedAuthority> result =
        ((Collection<?>) jwt.getClaims().getOrDefault("roles", emptyList()))
            .stream()
                .map(Object::toString)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

    String email = jwt.getClaimAsString("email");
    result.addAll(getAuthoritiesForEmail(accessStorageService, email));

    return result;
  }
}
