package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.molgenis.armadillo.metadata.UpdateSchedule;

public interface UpdatableContainer {
  @JsonProperty("autoUpdate")
  @Nullable
  Boolean getAutoUpdate();

  @JsonProperty("updateSchedule")
  @Nullable
  UpdateSchedule getUpdateSchedule();
}
