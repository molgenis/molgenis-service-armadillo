package org.molgenis.armadillo.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {
  private final Object apiKey;
  private final Object principal;

  public ApiKeyAuthentication(
      Object apiKey, Collection<? extends GrantedAuthority> authorities, Object principal) {
    super(authorities);
    this.apiKey = apiKey;
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  // TODO: we are misusing principal here. Use get name(?) and set proper token for this class
  @Override
  public Object getPrincipal() {
    String user = principal.toString();
    switch (principal) {
      case DefaultOAuth2User defaultOAuth2User -> user = defaultOAuth2User.getAttribute("email");
      case OAuth2User oAuth2User -> user = oAuth2User.getAttribute("email");
      case User user1 -> user = user1.getUsername();
      default -> {}
    }
    return user;
  }
}
