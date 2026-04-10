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
