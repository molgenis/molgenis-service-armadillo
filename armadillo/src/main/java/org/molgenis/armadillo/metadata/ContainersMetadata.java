package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class ContainersMetadata implements Persistable {
  @JsonProperty("profiles")
  public abstract ConcurrentMap<String, ContainerConfig> getProfiles();

  public static ContainersMetadata create() {
    return new AutoValue_ContainersMetadata(new ConcurrentHashMap<>());
  }

  @JsonCreator
  public static ContainersMetadata create(
      @JsonProperty("profiles") ConcurrentMap<String, ContainerConfig> newProfiles) {
    return new AutoValue_ContainersMetadata(newProfiles);
  }
}
