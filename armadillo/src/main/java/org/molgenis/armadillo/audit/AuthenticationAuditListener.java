package org.molgenis.armadillo.audit;

import static org.molgenis.armadillo.audit.AuditEventPublisher.getUser;
import static org.springframework.boot.actuate.security.AuthenticationAuditListener.AUTHENTICATION_SUCCESS;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.access.event.AuthorizationFailureEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener extends AbstractAuthenticationAuditListener {

  public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
  public static final String AUTHORIZATION_SUCCESS = "AUTHORIZATION_SUCCESS";

  @Override
  public void onApplicationEvent(@NotNull AbstractAuthenticationEvent event) {
    if (event instanceof AuthenticationSuccessEvent e) {
      onAuthenticationSuccessEvent(e);
    }
  }

  private void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
    publish(
        new AuditEvent(
            getUser(event.getAuthentication().getPrincipal()),
            AUTHENTICATION_SUCCESS,
            new HashMap<>()));
  }

  private void onAuthorizationFailureEvent(AuthorizationFailureEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", event.getAccessDeniedException().getClass().getName());
    data.put("message", event.getAccessDeniedException().getMessage());
    data.put("requestUrl", ((FilterInvocation) event.getSource()).getRequestUrl());

    if (event.getAuthentication().getDetails() != null) {
      data.put("details", event.getAuthentication().getDetails());
    }
    publish(new AuditEvent(event.getAuthentication().getName(), AUTHORIZATION_FAILURE, data));
  }
}
