package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;

public class AuthenticationService {

  private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

  public static Authentication getAuthentication(HttpServletRequest request, String authToken) {
    String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
    String authHeader = request.getHeader("Authorization");
    SecurityContextImpl context =
        (SecurityContextImpl) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");

    if (authHeader != null) {
      if (authHeader.startsWith("Bearer ") || authHeader.startsWith("Basic ")) {
        // TODO: Do we need to set authorities + authentication? -> get authentication from
        // header???
        final String jwtToken = authHeader.substring(7);
        return new ApiKeyAuthentication(authHeader, AuthorityUtils.NO_AUTHORITIES, jwtToken);
      }
    } else if (context != null
        && context.getAuthentication() != null
        && context.getAuthentication().getPrincipal() != null
        && context.getAuthentication().getPrincipal() != "anonymousUser"
        && context.getAuthentication().isAuthenticated()) {
      // TODO: check if this is sufficient (rename ApiKeyAuthentication???)
      // TODO: test with oauth user non admin
      return new ApiKeyAuthentication(
          context.getAuthentication().getCredentials(),
          context.getAuthentication().getAuthorities(),
          context.getAuthentication().getPrincipal());
    } else if (apiKey != null && apiKey.equals(authToken)) {
      // TODO: Do we need to set authorities here?
      return new ApiKeyAuthentication(
          apiKey,
          AuthorityUtils.NO_AUTHORITIES,
          new Principal() {
            @Override
            public String getName() {
              return "API_KEY";
            }
          });
    }
    return null;
  }
}
