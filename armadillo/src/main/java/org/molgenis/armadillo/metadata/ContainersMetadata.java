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

  // This is what the application uses internally for fast access
  @JsonIgnore
  public abstract ConcurrentMap<String, ContainerConfig> getContainers();

  // Jackson uses this to write the "containers" key to the JSON file
  @JsonProperty("containers")
  public Map<String, ContainerConfig> getContainerMap() {
    return getContainers();
  }

  @JsonCreator
  public static ContainersMetadata create(
      @JsonProperty("containers") Map<String, ContainerConfig> containerMap) {

    // SYSTEMIC DEBUG 1: Did Jackson find the key?
    if (containerMap == null) {
      System.out.println("!!! DEBUG: Jackson passed a NULL map to the creator");
    } else {
      System.out.println("!!! DEBUG: Jackson found " + containerMap.size() + " entries");
    }
    ConcurrentMap<String, ContainerConfig> map = new ConcurrentHashMap<>();
    if (containerMap != null) {
      // We ensure the internal map is populated from the JSON map
      containerMap.forEach(
          (key, config) -> {
            // If the config object doesn't have the name (it's in the key),
            // we could potentially set it here if ContainerConfig is mutable
            map.put(key, config);
          });
    }
    return new AutoValue_ContainersMetadata(map);
  }

  // Rename this from 'create' to 'createEmpty'
  public static ContainersMetadata createEmpty() {
    System.out.println("!!! DEBUG: Parent loader called createEmpty()");
    return new AutoValue_ContainersMetadata(new ConcurrentHashMap<>());
  }
}
