package org.molgenis.armadillo.security;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

class LoginAttemptListenerTest {

  @Mock private LoginAttemptTracker tracker;
  @Mock private AuthenticationFailureBadCredentialsEvent failureEvent;
  @Mock private AuthenticationSuccessEvent successEvent;

  private LoginAttemptListener listener;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    listener = new LoginAttemptListener(tracker);
  }

  @Test
  void testOnFailureRecordsFailure() {
    listener.onFailure(failureEvent);
    verify(tracker).recordFailure();
  }

  @Test
  void testOnSuccessRecordsSuccess() {
    listener.onSuccess(successEvent);
    verify(tracker).recordSuccess();
  }
}
