package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.molgenis.armadillo.container.ContainerConfig;

@AutoValue
public abstract class ContainersMetadata implements Persistable {

  @JsonIgnore
  public abstract ConcurrentMap<String, ContainerConfig> getContainers();

  @JsonProperty("containers")
  public Map<String, ContainerConfig> getContainerMap() {
    return getContainers();
  }

  @JsonCreator
  public static ContainersMetadata create(
      @JsonProperty("containers") Map<String, ContainerConfig> containerMap) {
    ConcurrentMap<String, ContainerConfig> map = new ConcurrentHashMap<>();
    if (containerMap != null) {
      map.putAll(containerMap);
    }
    return new AutoValue_ContainersMetadata(map);
  }

  public static ContainersMetadata create() {
    return new AutoValue_ContainersMetadata(new ConcurrentHashMap<>());
  }
}
