package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class AuthenticationFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private HttpSession session;
  @Mock private FilterChain filterChain;
  @Mock private Authentication authentication;

  private AuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    filter = new AuthenticationFilter();
    filter.setAuthToken("test-token");
    SecurityContextHolder.setContext(new SecurityContextImpl());

    when(request.getSession(true)).thenReturn(session);
  }

  @Test
  void testDoFilter_ActuatorProtectedEndpoint_SetsAuthentication() throws Exception {
    when(request.getRequestURI()).thenReturn("/actuator/env");

    mockStatic(AuthenticationService.class)
        .when(() -> AuthenticationService.getAuthentication(request, "test-token"))
        .thenReturn(authentication);

    filter.doFilter(request, response, filterChain);

    SecurityContext ctx = SecurityContextHolder.getContext();
    assertEquals(authentication, ctx.getAuthentication());
    verify(session).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilter_ActuatorAllowedEndpoint_NoAuthentication() throws Exception {
    when(request.getRequestURI()).thenReturn("/actuator/health");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilter_ExceptionHandling_ReturnsUnauthorized() throws Exception {
    when(request.getRequestURI()).thenThrow(new RuntimeException("Test Exception"));

    filter.doFilter(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("application/json");
  }
}
