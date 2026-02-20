package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class ContainersMetadata implements Persistable {
  @JsonProperty("containers")
  public abstract ConcurrentMap<String, ContainerConfig> getContainers();

  public static ContainersMetadata create() {
    return new AutoValue_ContainersMetadata(new ConcurrentHashMap<>());
  }

  @JsonCreator
  public static ContainersMetadata create(
      @JsonProperty("containers") ConcurrentMap<String, ContainerConfig> newContainers) {
    return new AutoValue_ContainersMetadata(newContainers);
  }
}
