package org.molgenis.armadillo.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

public class SystemSecurityToken extends UsernamePasswordAuthenticationToken {
  private static final List<GrantedAuthority> AUTHORITIES =
      AuthorityUtils.createAuthorityList("ROLE_SU");

  public SystemSecurityToken() {
    super("SYSTEM", "SYSTEM", AUTHORITIES);
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities() {
    return AUTHORITIES;
  }

  @Override
  public Object getCredentials() {
    return "SYSTEM";
  }

  @Override
  public Object getPrincipal() {
    return "SYSTEM";
  }
}
