package org.molgenis.datashield.jwt;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Extracts roles from JWT */
@Component
class JwtRolesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    return ((Collection<String>) jwt.getClaims().getOrDefault("roles", emptyList()))
        .stream()
            .map(it -> "ROLE_" + it)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
  }
}
