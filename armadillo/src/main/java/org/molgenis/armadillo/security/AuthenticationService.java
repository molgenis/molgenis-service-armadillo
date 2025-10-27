package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

public class AuthenticationService {

  private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

  public static Authentication getAuthentication(HttpServletRequest request, String authToken) {
    String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
    if (apiKey == null || !apiKey.equals(authToken)) {
      throw new BadCredentialsException("Invalid API Key");
    }

    return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
  }
}
