package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class ProfilesMetadata implements Persistable {
  @JsonProperty("profiles")
  public abstract ConcurrentMap<String, ProfileConfig> getProfiles();

  //  @JsonCreator
  public static ProfilesMetadata create() {
    return new AutoValue_ProfilesMetadata(new ConcurrentHashMap<>());
  }

  @JsonCreator
  public static ProfilesMetadata create(
      @JsonProperty("profiles") ConcurrentMap<String, ProfileConfig> newProfiles) {
    return new AutoValue_ProfilesMetadata(newProfiles);
  }
}
