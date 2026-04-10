package org.molgenis.armadillo.security;

import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;

class NoPopupBasicAuthenticationEntryPointTest {

  @Mock private LoginAttemptTracker tracker;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private NoPopupBasicAuthenticationEntryPoint entryPoint;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    entryPoint = new NoPopupBasicAuthenticationEntryPoint(tracker);
    entryPoint.setRealmName("Armadillo");
  }

  @Test
  void testReturns401WhenNotLocked() throws Exception {
    when(tracker.isLocked()).thenReturn(false);

    entryPoint.commence(request, response, new BadCredentialsException("bad"));

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response, never()).setContentType(anyString());
  }

  @Test
  void testReturnsLockoutJsonWhenLocked() throws Exception {
    when(tracker.isLocked()).thenReturn(true);
    when(tracker.getLockedUntil()).thenReturn(Instant.now().plusSeconds(60));

    StringWriter stringWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

    entryPoint.commence(request, response, new BadCredentialsException("bad"));

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("application/json");
    verify(response).setHeader(eq("Retry-After"), anyString());

    String body = stringWriter.toString();
    assert body.contains("\"locked\":true");
    assert body.contains("\"secondsRemaining\":");
  }
}
