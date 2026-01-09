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
        final String jwtToken = authHeader.substring(7);
        return new KeyAuthentication(authHeader, AuthorityUtils.NO_AUTHORITIES, jwtToken);
      }
    } else if (context != null
        && context.getAuthentication() != null
        && context.getAuthentication().getPrincipal() != null
        && context.getAuthentication().getPrincipal() != "anonymousUser"
        && context.getAuthentication().isAuthenticated()) {
      return new KeyAuthentication(
          context.getAuthentication().getCredentials(),
          context.getAuthentication().getAuthorities(),
          context.getAuthentication().getPrincipal());
    } else if (apiKey != null && apiKey.equals(authToken)) {
      return new KeyAuthentication(
          apiKey, AuthorityUtils.NO_AUTHORITIES, (Principal) () -> "API_KEY");
    }
    System.out.println("authHeader: " + authHeader);
    System.out.println("context: " + context);
    if (context != null) {
      System.out.println(context.getAuthentication());
      if (context.getAuthentication() != null) {
        System.out.println("principal: " + context.getAuthentication().getPrincipal());
        System.out.println("authority: " + context.getAuthentication().getAuthorities());
        System.out.println("credentials: " + context.getAuthentication().getCredentials());
      }
    }
    System.out.println("apiKey: " + apiKey);
    return null;
  }
}
