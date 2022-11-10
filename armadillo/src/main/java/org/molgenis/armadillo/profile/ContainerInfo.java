package org.molgenis.armadillo.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;
import org.molgenis.armadillo.metadata.ProfileStatus;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ContainerInfo {
  @Nullable // only present when container exists and Docker is online
  public abstract List<String> getTags();

  public abstract ProfileStatus getStatus();

  public static ContainerInfo create(ProfileStatus status) {
    return new AutoValue_ContainerInfo(null, status);
  }

  public static ContainerInfo create(List<String> tags, ProfileStatus status) {
    return new AutoValue_ContainerInfo(tags, status);
  }
}
