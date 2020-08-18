package org.molgenis.r.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.molgenis.r.model.RPackage.Builder;

@AutoValue
@JsonSerialize(as = RProcess.class)
@JsonInclude(Include.NON_NULL)
public abstract class RProcess {
  public enum Status {
    IDLE,
    RUNNING,
    SLEEPING,
    DISK_SLEEP,
    STOPPED,
    TRACING_STOP,
    ZOMBIE,
    DEAD,
    WAKE_KILL,
    WAKING;
  }

  @Nullable
  @JsonProperty("pid")
  public abstract Integer pid();

  @Nullable
  @JsonProperty("ppid")
  public abstract Integer pPid();

  @Nullable
  @JsonProperty("name")
  public abstract String name();

  @Nullable
  @JsonProperty("cmd")
  public abstract String cmd();

  @Nullable
  @JsonProperty("username")
  public abstract String username();

  @JsonProperty("ports")
  public abstract List<Integer> ports();

  @Nullable
  @JsonProperty("status")
  public abstract Status status();

  @Nullable
  @JsonProperty("user")
  public abstract Double user();

  @Nullable
  @JsonProperty("system")
  public abstract Double system();

  @Nullable
  @JsonProperty("rss")
  public abstract Double rss();

  @Nullable
  @JsonProperty("vms")
  public abstract Double vms();

  @Nullable
  @JsonProperty("created")
  public abstract Instant created();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPid(Integer pid);

    public abstract Builder setPPid(Integer ppid);

    public abstract Builder setName(String name);

    public abstract Builder setCmd(String name);

    public abstract Builder setUsername(String username);

    public abstract Builder setPorts(List<Integer> ports);

    public abstract Builder setStatus(Status status);

    public abstract Builder setUser(Double user);

    public abstract Builder setSystem(Double system);

    public abstract Builder setRss(Double rss);

    public abstract Builder setVms(Double vms);

    public abstract Builder setCreated(Instant created);

    public abstract RProcess build();
  }

  public static Builder builder() {
    return new AutoValue_RProcess.Builder().setPorts(List.of());
  }
}
