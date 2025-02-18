package org.molgenis.armadillo.info;

import java.security.Principal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class UserInformationRetriever {
  static final String EMAIL = "email";
  static final String ANONYMOUS = "ANONYMOUS";

  public static String getUser(Object principal) {
    if (principal == null) {
      return ANONYMOUS;
    } else if (principal instanceof OAuth2AuthenticationToken token) {
      return token.getPrincipal().getAttribute(EMAIL);
    } else if (principal instanceof JwtAuthenticationToken token) {
      return token.getTokenAttributes().get(EMAIL).toString();
    } else if (principal instanceof DefaultOAuth2User user) {
      return user.getAttributes().get(EMAIL).toString();
    } else if (principal instanceof Jwt jwt) {
      return jwt.getClaims().get(EMAIL).toString();
    } else if (principal instanceof User user) {
      return user.getUsername();
    } else if (principal instanceof Principal p) {
      return p.getName();
    } else {
      return principal.toString();
    }
  }
}
