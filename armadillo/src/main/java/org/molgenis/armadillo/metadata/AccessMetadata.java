package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class AccessMetadata implements Persistable {
  @JsonProperty("users")
  public abstract ConcurrentMap<String, UserDetails> getUsers();

  @JsonProperty("projects")
  public abstract ConcurrentMap<String, ProjectDetails> getProjects();

  @JsonProperty("permissions")
  public abstract Set<ProjectPermission> getPermissions();

  @JsonCreator
  public static AccessMetadata create() {
    return new AutoValue_AccessMetadata(
        new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new HashSet<>());
  }

  @JsonCreator
  public static AccessMetadata create(
      @JsonProperty("users") ConcurrentMap<String, UserDetails> newUsers,
      @JsonProperty("projects") ConcurrentMap<String, ProjectDetails> newProjects,
      @JsonProperty("permissions") Set<ProjectPermission> newPermissions) {
    return new AutoValue_AccessMetadata(newUsers, newProjects, newPermissions);
  }
}
