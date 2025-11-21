package org.molgenis.armadillo.security;

import java.security.Principal;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

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
    String user = principal.toString();
    switch (principal) {
      case DefaultOAuth2User defaultOAuth2User -> user = defaultOAuth2User.getAttribute("email");
      case OAuth2User oAuth2User -> user = oAuth2User.getAttribute("email");
      case User user1 -> user = user1.getUsername();
      default -> {
        try {
          user = ((Principal) principal).getName();
        } catch (Exception ignored) {
        }
      }
    }
    return user;
  }
}
