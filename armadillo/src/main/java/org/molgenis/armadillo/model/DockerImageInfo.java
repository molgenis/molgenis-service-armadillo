package org.molgenis.armadillo.model;

import static org.molgenis.armadillo.storage.LocalStorageService.getFileSizeInUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.model.Image;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import java.util.*;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DockerImageInfo {

  @NotEmpty
  public abstract Image image();

  @JsonProperty("created")
  public Date getCreated() {
    return new Date(image().getCreated() * 1000);
  }

  @JsonProperty("size")
  public String getSize() {
    return getFileSizeInUnit(image().getSize());
  }

  @JsonProperty("imageId")
  public String getImageId() {
    return image().getId();
  }

  @JsonProperty("repoTags")
  public String[] getRepoTags() {
    return image().getRepoTags();
  }

  @JsonCreator
  public static DockerImageInfo create(Image image) {
    return new AutoValue_DockerImageInfo(image);
  }
}
