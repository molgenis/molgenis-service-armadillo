package org.molgenis.armadillo.audit;

import static org.molgenis.armadillo.audit.AuditEventPublisher.getUser;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener extends AbstractAuthenticationAuditListener {

  public static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";
  public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";

  @Override
  public void onApplicationEvent(@NotNull AbstractAuthenticationEvent event) {
    if (event instanceof AuthenticationSuccessEvent e) {
      onAuthenticationSuccessEvent(e);
    } else if (event instanceof LogoutSuccessEvent e) {
      onLogoutSuccessEvent(e);
    }
  }

  private void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
    publish(
        new AuditEvent(
            getUser(event.getAuthentication().getPrincipal()),
            AUTHENTICATION_SUCCESS,
            Map.of(
                "details",
                event.getAuthentication().getDetails(),
                "authorities",
                event.getAuthentication().getAuthorities())));
  }

  private void onLogoutSuccessEvent(LogoutSuccessEvent event) {
    publish(
        new AuditEvent(
            getUser(event.getAuthentication().getPrincipal()),
            LOGOUT_SUCCESS,
            Map.of(
                "details",
                event.getAuthentication().getDetails(),
                "authorities",
                event.getAuthentication().getAuthorities())));
  }
}
