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
    return switch (principal) {
      case null -> ANONYMOUS;
      case OAuth2AuthenticationToken token -> token.getPrincipal().getAttribute(EMAIL);
      case JwtAuthenticationToken token -> token.getTokenAttributes().get(EMAIL).toString();
      case DefaultOAuth2User user -> user.getAttribute(EMAIL);
      case Jwt jwt -> jwt.getClaims().get(EMAIL).toString();
      case User user -> user.getUsername();
      case Principal p -> p.getName();
      default -> principal.toString();
    };
  }
}
