package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;

class AuthenticationServiceTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpSession session;
  @Mock private Authentication authentication;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(request.getSession()).thenReturn(session);
    when(request.getSession().getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(null);
  }

  @Test
  void testGetAuthentication_BearerToken() {
    when(request.getHeader("Authorization")).thenReturn("Bearer abcdef123456");
    System.out.println(request.getHeader("Authorization"));
    Authentication auth = AuthenticationService.getAuthentication(request, "ignored-token");
    assertNotNull(auth);
    assertEquals("Bearer abcdef123456", auth.getCredentials());
  }

  @Test
  void testGetAuthentication_BasicAuth() {
    when(request.getHeader("Authorization")).thenReturn("Basic ZXhhbXBsZTp0ZXN0");
    Authentication auth = AuthenticationService.getAuthentication(request, "ignored-token");
    assertNotNull(auth);
    assertEquals("Basic ZXhhbXBsZTp0ZXN0", auth.getCredentials());
  }

  @Test
  void testGetAuthentication_ContextAuthenticationValid() {
    when(request.getSession()).thenReturn(session);

    SecurityContextImpl context = new SecurityContextImpl();
    when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(context);

    when(authentication.getPrincipal()).thenReturn("someUser");
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getCredentials()).thenReturn("cred123");
    when(authentication.getAuthorities()).thenReturn((Collection) AuthorityUtils.NO_AUTHORITIES);

    context.setAuthentication(authentication);

    Authentication auth = AuthenticationService.getAuthentication(request, "ignored-token");

    assertNotNull(auth);
    assertEquals("cred123", auth.getCredentials());
    assertEquals("someUser", auth.getPrincipal());
  }

  @Test
  void testGetAuthentication_ApiKeyMatch() {
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getHeader("X-API-KEY")).thenReturn("correct-token");

    Authentication auth = AuthenticationService.getAuthentication(request, "correct-token");

    assertNotNull(auth);
    assertEquals("correct-token", auth.getCredentials());
    assertEquals("API_KEY", auth.getPrincipal());
  }

  @Test
  void testGetAuthentication_NoValidAuth_ReturnsNull() {
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getHeader("X-API-KEY")).thenReturn("wrong-token");

    Authentication auth = AuthenticationService.getAuthentication(request, "correct-token");

    assertNull(auth);
  }
}
