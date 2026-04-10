package org.molgenis.armadillo.security;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptTracker {

  private static final Logger logger = LoggerFactory.getLogger(LoginAttemptTracker.class);

  static final int FREE_ATTEMPTS = 5;
  static final Duration BASE_LOCKOUT = Duration.ofMinutes(1);
  static final Duration MAX_LOCKOUT = Duration.ofMinutes(30);

  private int failedAttempts;
  private Instant lockedUntil = null;

  public synchronized void recordFailure() {
    failedAttempts++;
    if (failedAttempts > FREE_ATTEMPTS) {
      int over = failedAttempts - FREE_ATTEMPTS;
      long multiplier = (long) Math.pow(2, over - 1);
      Duration lockout = BASE_LOCKOUT.multipliedBy(multiplier);
      if (lockout.compareTo(MAX_LOCKOUT) > 0) {
        lockout = MAX_LOCKOUT;
      }
      lockedUntil = Instant.now().plus(lockout);
      logger.warn(
          "Admin account locked until {} after {} failed attempts", lockedUntil, failedAttempts);
    }
  }

  public synchronized void recordSuccess() {
    failedAttempts = 0;
    lockedUntil = null;
  }

  public synchronized boolean isLocked() {
    return lockedUntil != null && Instant.now().isBefore(lockedUntil);
  }

  public synchronized Instant getLockedUntil() {
    return lockedUntil;
  }
}
