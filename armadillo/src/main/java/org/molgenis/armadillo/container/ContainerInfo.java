package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.List;
import org.molgenis.armadillo.metadata.ContainerStatus;

@AutoValue
@JsonInclude(Include.NON_NULL)
public abstract class ContainerInfo {
  public static ContainerInfo create(List<String> tags, ContainerStatus status) {
    return new AutoValue_ContainerInfo(tags, status, null);
  }

  @Nullable // only present when container exists and Docker is online
  public abstract List<String> getTags();

  public abstract ContainerStatus getStatus();

  @Nullable
  public abstract String getImageId();

  public static ContainerInfo create(ContainerStatus status) {
    return new AutoValue_ContainerInfo(null, status, null);
  }

  public static ContainerInfo create(
      List<String> tags, ContainerStatus status, @Nullable String imageId) {
    return new AutoValue_ContainerInfo(tags, status, imageId);
  }
}
