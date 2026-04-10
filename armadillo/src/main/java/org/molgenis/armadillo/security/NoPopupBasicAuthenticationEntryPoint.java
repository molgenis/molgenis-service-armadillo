package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class NoPopupBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

  private final LoginAttemptTracker tracker;

  public NoPopupBasicAuthenticationEntryPoint(LoginAttemptTracker tracker) {
    this.tracker = tracker;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    if (authException instanceof LockedException) {
      writeLockoutResponse(response);
    }
  }

  private void writeLockoutResponse(HttpServletResponse response) throws IOException {
    Instant lockedUntil = tracker.getLockedUntil();
    long secondsRemaining = Duration.between(Instant.now(), lockedUntil).getSeconds();
    response.setContentType("application/json");
    response.setHeader("Retry-After", String.valueOf(secondsRemaining));
    response
        .getWriter()
        .write(
            String.format(
                "{\"locked\":true,\"lockedUntil\":\"%s\",\"secondsRemaining\":%d}",
                lockedUntil, secondsRemaining));
  }

  @Override
  public void afterPropertiesSet() {
    setRealmName("Armadillo");
    super.afterPropertiesSet();
  }
}
