package org.molgenis.armadillo.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class KeyAuthentication extends AbstractAuthenticationToken {
  private final Object key;
  private final Object principal;

  public KeyAuthentication(
      Object key, Collection<? extends GrantedAuthority> authorities, Object principal) {
    super(authorities);
    this.key = key;
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return key;
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }
}
