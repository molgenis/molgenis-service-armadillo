package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
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
        // TODO: Do we need to set authorities?
        return new ApiKeyAuthentication(authHeader, AuthorityUtils.NO_AUTHORITIES);
      }
    } else if (context != null
            && context.getAuthentication() != null
            && context.getAuthentication().getPrincipal() != null
            && context.getAuthentication().getPrincipal() != "anonymousUser"
            && context.getAuthentication().isAuthenticated()) {
      // TODO: check if this is sufficient (rename ApiKeyAuthentication???)
      // TODO: FIGURE OUT WHY OAUTH LOGOUT STILL AUTHENTICATES here, scenario: login using oauth, go
      // to metrics page, logout on auth server, refresh, expected: metrics still show, but user
      // isn't present, but getting:
      //  Name: [677a49c1-8702-42fe-8a71-3e922536a76a], Granted Authorities: [[OAUTH2_USER]], User
      // Attributes: [{applicationId=b396233b-cdb2-449e-ac5c-a0d28b38f791,
      // email=m.k.slofstra@umcg.nl, email_verified=true, family_name=Slofstra, given_name=Mariska,
      // name=Mariska Slofstra (medgen), roles=[SU], sub=677a49c1-8702-42fe-8a71-3e922536a76a}]
      // TODO: test with oauth user non admin
      return new ApiKeyAuthentication(
              context.getAuthentication().getPrincipal().toString(),
              context.getAuthentication().getAuthorities());
    } else if (apiKey != null && apiKey.equals(authToken)) {
      // TODO: Do we need to set authorities here?
      return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
    }
    return null;
  }
}