package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;

public interface OpenContainersConfig {
  @Nullable
  String getVersionId();

  @Nullable
  String getCreationDate();
}
