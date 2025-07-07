package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public class AutoUpdateSchedule {
  public String frequency;
  @Nullable public String day;
  @Nullable public String time;

  public AutoUpdateSchedule() {}

  public AutoUpdateSchedule(String frequency, String day, String time) {
    this.frequency = frequency;
    this.day = day;
    this.time = time;
  }

  @Override
  public String toString() {
    return "AutoUpdateSchedule{"
        + "frequency='"
        + frequency
        + '\''
        + ", day='"
        + day
        + '\''
        + ", time='"
        + time
        + '\''
        + '}';
  }
}
