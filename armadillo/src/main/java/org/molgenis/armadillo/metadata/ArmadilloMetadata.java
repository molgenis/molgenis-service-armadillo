package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class ArmadilloMetadata {
  @JsonProperty("users")
  public abstract ConcurrentMap<String, UserDetails> getUsers();

  @JsonProperty("projects")
  public abstract ConcurrentMap<String, ProjectDetails> getProjects();

  @JsonProperty("profiles")
  public abstract ConcurrentMap<String, ProfileConfig> getProfiles();

  @JsonProperty("permissions")
  public abstract Set<ProjectPermission> getPermissions();

  @JsonCreator
  public static ArmadilloMetadata create() {
    return new AutoValue_ArmadilloMetadata(
        new ConcurrentHashMap<>(),
        new ConcurrentHashMap<>(),
        new ConcurrentHashMap<>(),
        new HashSet<>());
  }

  @JsonCreator
  public static ArmadilloMetadata create(
      @JsonProperty("users") ConcurrentMap<String, UserDetails> newUsers,
      @JsonProperty("projects") ConcurrentMap<String, ProjectDetails> newProjects,
      @JsonProperty("profiles") ConcurrentMap<String, ProfileConfig> newProfiles,
      @JsonProperty("permissions") Set<ProjectPermission> newPermissions) {
    return new AutoValue_ArmadilloMetadata(newUsers, newProjects, newProfiles, newPermissions);
  }
}
