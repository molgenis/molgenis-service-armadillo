package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public interface OpenContainer {
  @JsonProperty("versionId")
  @Nullable
  String getVersionId();

  @JsonProperty("creationDate")
  @Nullable
  String getCreationDate();
}
