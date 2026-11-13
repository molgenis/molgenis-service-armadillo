package org.molgenis.armadillo.container;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

// AutoValue requires redeclaring interface methods as abstract - suppress S1161 and S3038
@SuppressWarnings({"java:S1161", "java:S3038"})
public abstract class AbstractFlowerContainerConfig
    implements ContainerConfig, OpenContainer, FlowerContainer {

  @Override
  public abstract String getName();

  @Override
  public abstract String getImage();

  @Override
  @Nullable
  public abstract String getHost();

  @Override
  @Nullable
  public abstract Integer getPort();

  @Override
  @Nullable
  public abstract Long getImageSize();

  @Override
  @Nullable
  public abstract String getInstallDate();

  @Override
  @Nullable
  public abstract String getLastImageId();

  @Override
  @Nullable
  public abstract List<String> getDockerArgs();

  @Override
  @Nullable
  public abstract Map<String, Object> getDockerOptions();

  @Override
  @Nullable
  public abstract String getVersionId();

  @Override
  @Nullable
  public abstract String getCreationDate();

  public abstract static class Builder<B extends Builder<B>> {
    public abstract B name(String name);

    public abstract B image(String image);

    public abstract B host(@Nullable String host);

    public abstract B port(@Nullable Integer port);

    public abstract B imageSize(@Nullable Long imageSize);

    public abstract B installDate(@Nullable String installDate);

    public abstract B lastImageId(@Nullable String lastImageId);

    public abstract B dockerArgs(@Nullable List<String> dockerArgs);

    public abstract B dockerOptions(@Nullable Map<String, Object> dockerOptions);

    public abstract B versionId(@Nullable String versionId);

    public abstract B creationDate(@Nullable String creationDate);
  }
}
