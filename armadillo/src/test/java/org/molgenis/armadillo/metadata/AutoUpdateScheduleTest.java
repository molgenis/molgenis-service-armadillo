package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AutoUpdateScheduleTest {

  @Test
  void testNullArgsConstructor() {
    var schedule = new AutoUpdateSchedule(null, null, null);

    assertNull(schedule.frequency());
    assertNull(schedule.day());
    assertNull(schedule.time());
  }

  @Test
  void testAllArgsConstructor() {
    var schedule = new AutoUpdateSchedule("Weekly", "Monday", "10:00");

    assertEquals("Weekly", schedule.frequency());
    assertEquals("Monday", schedule.day());
    assertEquals("10:00", schedule.time());
  }
}
