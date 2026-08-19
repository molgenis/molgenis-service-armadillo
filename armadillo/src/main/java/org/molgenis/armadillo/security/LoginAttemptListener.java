package org.molgenis.armadillo.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptListener {
  private final LoginAttemptTracker tracker;

  public LoginAttemptListener(LoginAttemptTracker tracker) {
    this.tracker = tracker;
  }

  @EventListener
  public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
    tracker.recordFailure();
  }

  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    tracker.recordSuccess();
  }
}
