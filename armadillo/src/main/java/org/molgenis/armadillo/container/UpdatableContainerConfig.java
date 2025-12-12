package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.UpdateSchedule;

public interface UpdatableContainerConfig {

  Boolean getAutoUpdate();

  UpdateSchedule getUpdateSchedule();
}
