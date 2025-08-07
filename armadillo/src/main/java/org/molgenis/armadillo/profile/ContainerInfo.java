package org.molgenis.armadillo.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import org.molgenis.armadillo.metadata.ProfileStatus;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ContainerInfo {
  public static ContainerInfo create(List<String> tags, ProfileStatus status) {
    return new AutoValue_ContainerInfo(tags, status, null);
  }

  @Nullable // only present when container exists and Docker is online
  public abstract List<String> getTags();

  public abstract ProfileStatus getStatus();

  @Nullable
  public abstract String getImageId();

  public static ContainerInfo create(ProfileStatus status) {
    return new AutoValue_ContainerInfo(null, status, null);
  }

  public static ContainerInfo create(
      List<String> tags, ProfileStatus status, @Nullable String imageId) {
    return new AutoValue_ContainerInfo(tags, status, imageId);
  }
}
