package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.StringUtils;

public class AuthenticationService {

  private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

  public static Authentication getAuthentication(HttpServletRequest request, String authToken) {

    String authHeader =
        StringUtils.hasText(request.getHeader("Authorization"))
            ? request.getHeader("Authorization")
            : null;

    String apiKey =
        StringUtils.hasText(request.getHeader(AUTH_TOKEN_HEADER_NAME))
            ? request.getHeader(AUTH_TOKEN_HEADER_NAME)
            : null;

    HttpSession session = request.getSession(false);
    SecurityContextImpl context =
        session != null
            ? (SecurityContextImpl) session.getAttribute("SPRING_SECURITY_CONTEXT")
            : null;

    if (authHeader != null
        && (authHeader.startsWith("Bearer ") || authHeader.startsWith("Basic "))) {

      String token = authHeader.substring(authHeader.indexOf(' ') + 1);
      return new KeyAuthentication(authHeader, AuthorityUtils.NO_AUTHORITIES, token);
    }

    if (context != null
        && context.getAuthentication() != null
        && context.getAuthentication().isAuthenticated()
        && !(context.getAuthentication() instanceof AnonymousAuthenticationToken)) {

      return new KeyAuthentication(
          context.getAuthentication().getCredentials(),
          context.getAuthentication().getAuthorities(),
          context.getAuthentication().getPrincipal());
    }

    if (apiKey != null && apiKey.equals(authToken)) {
      return new KeyAuthentication(
          apiKey, AuthorityUtils.NO_AUTHORITIES, (Principal) () -> "API_KEY");
    }

    return null;
  }
}
