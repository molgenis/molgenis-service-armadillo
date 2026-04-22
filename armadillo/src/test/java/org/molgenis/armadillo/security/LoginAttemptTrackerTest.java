package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;

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
}
