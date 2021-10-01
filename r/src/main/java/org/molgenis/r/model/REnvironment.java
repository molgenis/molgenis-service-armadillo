package org.molgenis.r.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonSerialize(as = REnvironment.class)
public abstract class REnvironment {
  @JsonSerialize
  public abstract String name();
  @JsonSerialize
  public abstract List<RProcess> processes();

  public static REnvironment create(String name, List<RProcess> processes) {
    return new AutoValue_REnvironment(name, processes);
  }

}
