package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptTrackerTest {

  private LoginAttemptTracker tracker;

  @BeforeEach
  void setUp() {
    tracker = new LoginAttemptTracker();
  }

  @Test
  void testNotLockedInitially() {
    assertFalse(tracker.isLocked());
    assertNull(tracker.getLockedUntil());
  }

  @Test
  void testNotLockedWithinFreeAttempts() {
    for (int i = 0; i < LoginAttemptTracker.FREE_ATTEMPTS; i++) {
      tracker.recordFailure();
    }
    assertFalse(tracker.isLocked());
  }

  @Test
  void testLockedAfterExceedingFreeAttempts() {
    for (int i = 0; i <= LoginAttemptTracker.FREE_ATTEMPTS; i++) {
      tracker.recordFailure();
    }
    assertTrue(tracker.isLocked());
    assertNotNull(tracker.getLockedUntil());
  }

  @Test
  void testSuccessResetsCounter() {
    for (int i = 0; i <= LoginAttemptTracker.FREE_ATTEMPTS; i++) {
      tracker.recordFailure();
    }
    assertTrue(tracker.isLocked());

    tracker.recordSuccess();
    assertFalse(tracker.isLocked());
    assertNull(tracker.getLockedUntil());
  }

  @Test
  void testAttemptsRemainingDecreasesWithFailures() {
    assertEquals(LoginAttemptTracker.FREE_ATTEMPTS, tracker.getAttemptsRemaining());

    tracker.recordFailure();
    assertEquals(LoginAttemptTracker.FREE_ATTEMPTS - 1, tracker.getAttemptsRemaining());

    for (int i = 1; i < LoginAttemptTracker.FREE_ATTEMPTS; i++) {
      tracker.recordFailure();
    }
    assertEquals(0, tracker.getAttemptsRemaining());

    // Should not go below 0
    tracker.recordFailure();
    assertEquals(0, tracker.getAttemptsRemaining());
  }

  @Test
  void testAttemptsRemainingResetsOnSuccess() {
    for (int i = 0; i < 3; i++) {
      tracker.recordFailure();
    }
    assertEquals(LoginAttemptTracker.FREE_ATTEMPTS - 3, tracker.getAttemptsRemaining());

    tracker.recordSuccess();
    assertEquals(LoginAttemptTracker.FREE_ATTEMPTS, tracker.getAttemptsRemaining());
  }

  @Test
  void testLockoutDurationIncreases() {
    // First lockout: 1 minute
    for (int i = 0; i <= LoginAttemptTracker.FREE_ATTEMPTS; i++) {
      tracker.recordFailure();
    }
    var firstLockout = tracker.getLockedUntil();

    // Second lockout: 2 minutes
    tracker.recordFailure();
    var secondLockout = tracker.getLockedUntil();

    assertTrue(secondLockout.isAfter(firstLockout));
  }

  @Test
  void testLockoutCappedAtMax() {
    // 5 free attempts + 6 lockouts. The 6th tier would be 32 minutes (BASE_LOCKOUT << 5),
    // which exceeds MAX_LOCKOUT (30 minutes), so it must be capped.
    for (int i = 0; i <= LoginAttemptTracker.FREE_ATTEMPTS + 5; i++) {
      tracker.recordFailure();
    }

    Instant lockedUntil = tracker.getLockedUntil();
    // Capped: must not exceed now + MAX_LOCKOUT...
    assertFalse(lockedUntil.isAfter(Instant.now().plus(LoginAttemptTracker.MAX_LOCKOUT)));
    // ...and must be at the cap, not the smaller previous tier.
    assertTrue(lockedUntil.isAfter(Instant.now().plus(Duration.ofMinutes(29))));
  }
}
