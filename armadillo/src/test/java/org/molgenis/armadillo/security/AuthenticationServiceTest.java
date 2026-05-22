package org.molgenis.armadillo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;

class AuthenticationServiceTest {

  private static final String VALID_API_KEY = "test-api-key";

  private HttpServletRequest request;
  private HttpSession session;

  @BeforeEach
  void setUp() {
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession()).thenReturn(session);
    // Default: no headers, no session context
    when(request.getHeader("X-API-KEY")).thenReturn(null);
    when(request.getHeader("Authorization")).thenReturn(null);
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(null);
  }

  // -------------------------------------------------------------------------
  // Bearer token
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_returnsKeyAuthentication_forBearerToken() {
    when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);
    assertThat(auth).isInstanceOf(KeyAuthentication.class);
  }

  @Test
  void getAuthentication_preservesFullAuthorizationHeader_forBearerToken() {
    when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    // Credentials hold the full header value
    assertThat(auth.getCredentials()).isEqualTo("Bearer some.jwt.token");
  }

  @Test
  void getAuthentication_hasNoAuthorities_forBearerToken() {
    when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getAuthorities()).isEqualTo(AuthorityUtils.NO_AUTHORITIES);
  }

  // -------------------------------------------------------------------------
  // Basic auth header
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_returnsKeyAuthentication_forBasicAuthHeader() {
    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);
    assertThat(auth).isInstanceOf(KeyAuthentication.class);
  }

  @Test
  void getAuthentication_preservesFullAuthorizationHeader_forBasicAuth() {
    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getCredentials()).isEqualTo("Basic dXNlcjpwYXNz");
  }

  @Test
  void getAuthentication_returnsNull_forUnrecognizedAuthorizationScheme() {
    when(request.getHeader("Authorization")).thenReturn("Digest something");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  // -------------------------------------------------------------------------
  // Session context
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_returnsKeyAuthentication_fromSessionContext() {
    when(session.getAttribute("SPRING_SECURITY_CONTEXT"))
        .thenReturn(sessionContextWithAuthenticatedUser("user", "creds"));

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);
    assertThat(auth).isInstanceOf(KeyAuthentication.class);
  }

  @Test
  void getAuthentication_propagatesCredentialsFromSession() {
    when(session.getAttribute("SPRING_SECURITY_CONTEXT"))
        .thenReturn(sessionContextWithAuthenticatedUser("user", "session-creds"));

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getCredentials()).isEqualTo("session-creds");
  }

  @Test
  void getAuthentication_returnsNull_whenSessionContextHasAnonymousUser() {
    Authentication anonymous =
        new UsernamePasswordAuthenticationToken(
            "anonymousUser", null, AuthorityUtils.NO_AUTHORITIES);
    SecurityContextImpl context = new SecurityContextImpl(anonymous);
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(context);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  @Test
  void getAuthentication_returnsNull_whenSessionContextAuthenticationIsNull() {
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl());

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  @Test
  void getAuthentication_returnsNull_whenSessionContextIsNull() {
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(null);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  @Test
  void getAuthentication_returnsNull_whenSessionAuthenticationIsNotAuthenticated() {
    Authentication unauthenticated = mock(Authentication.class);
    when(unauthenticated.isAuthenticated()).thenReturn(false);
    when(unauthenticated.getPrincipal()).thenReturn("user");
    SecurityContextImpl context = new SecurityContextImpl(unauthenticated);
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(context);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  // -------------------------------------------------------------------------
  // API key
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_returnsKeyAuthentication_forValidApiKey() {
    when(request.getHeader("X-API-KEY")).thenReturn(VALID_API_KEY);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);
    assertThat(auth).isInstanceOf(KeyAuthentication.class);
  }

  @Test
  void getAuthentication_apiKey_principalNameIsApiKey() {
    when(request.getHeader("X-API-KEY")).thenReturn(VALID_API_KEY);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getName()).isEqualTo("API_KEY");
  }

  @Test
  void getAuthentication_returnsNull_forInvalidApiKey() {
    when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  @Test
  void getAuthentication_returnsNull_whenApiKeyHeaderPresentButAuthTokenIsNull() {
    when(request.getHeader("X-API-KEY")).thenReturn("some-key");

    Authentication auth = AuthenticationService.getAuthentication(request, null);

    assertThat(auth).isNull();
  }

  // -------------------------------------------------------------------------
  // Priority: Authorization header takes precedence over session and API key
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_authorizationHeaderTakesPrecedenceOverSession() {
    when(request.getHeader("Authorization")).thenReturn("Bearer jwt.token");
    when(session.getAttribute("SPRING_SECURITY_CONTEXT"))
        .thenReturn(sessionContextWithAuthenticatedUser("user", "creds"));

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    // Bearer wins — credentials are the full Authorization header value
    assertThat(auth.getCredentials()).isEqualTo("Bearer jwt.token");
  }

  @Test
  void getAuthentication_authorizationHeaderTakesPrecedenceOverApiKey() {
    when(request.getHeader("Authorization")).thenReturn("Bearer jwt.token");
    when(request.getHeader("X-API-KEY")).thenReturn(VALID_API_KEY);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getCredentials()).isEqualTo("Bearer jwt.token");
  }

  @Test
  void getAuthentication_sessionTakesPrecedenceOverApiKey() {
    when(session.getAttribute("SPRING_SECURITY_CONTEXT"))
        .thenReturn(sessionContextWithAuthenticatedUser("user", "session-creds"));
    when(request.getHeader("X-API-KEY")).thenReturn(VALID_API_KEY);

    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth.getCredentials()).isEqualTo("session-creds");
  }

  // -------------------------------------------------------------------------
  // No credentials at all
  // -------------------------------------------------------------------------

  @Test
  void getAuthentication_returnsNull_whenNoCredentialsPresent() {
    Authentication auth = AuthenticationService.getAuthentication(request, VALID_API_KEY);

    assertThat(auth).isNull();
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private static SecurityContextImpl sessionContextWithAuthenticatedUser(
      String principal, String credentials) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            principal, credentials, AuthorityUtils.createAuthorityList("ROLE_USER"));
    return new SecurityContextImpl(authentication);
  }
}
