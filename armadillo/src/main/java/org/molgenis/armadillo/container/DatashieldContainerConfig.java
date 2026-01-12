package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.UpdateSchedule;
import org.molgenis.r.config.EnvironmentConfigProps;

@AutoValue
@JsonTypeName("ds")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DatashieldContainerConfig
    implements ContainerConfig, UpdatableContainer, OpenContainer {

  public abstract Set<String> getPackageWhitelist();

  public abstract Set<String> getFunctionBlacklist();

  public abstract Map<String, String> getOptions();

  @JsonCreator
  public static DatashieldContainerConfig create(
      String name,
      @Nullable String image,
      @Nullable String host,
      @Nullable Integer port,
      @Nullable String lastImageId,
      @Nullable Long imageSize,
      @Nullable String installDate,
      @Nullable String versionId,
      @Nullable String creationDate,
      @Nullable Boolean autoUpdate,
      @Nullable UpdateSchedule updateSchedule,
      @Nullable Set<String> packageWhitelist,
      @Nullable Set<String> functionBlacklist,
      @Nullable Map<String, String> options) {

    return builder()
        .name(name)
        .image(image)
        .host(host)
        .port(port)
        .lastImageId(lastImageId)
        .imageSize(imageSize)
        .installDate(installDate)
        .versionId(versionId)
        .creationDate(creationDate)
        .autoUpdate(autoUpdate)
        .updateSchedule(updateSchedule)
        .packageWhitelist(packageWhitelist)
        .functionBlacklist(functionBlacklist)
        .options(options)
        .build();
  }

  @Override
  public String getType() {
    return "ds";
  }

  public static DatashieldContainerConfig createDefault() {
    return builder().name("default").build();
  }

  @Override
  public Map<String, Object> getSpecificContainerConfig() {
    return Map.of(
        "packageWhitelist", getPackageWhitelist(),
        "functionBlacklist", getFunctionBlacklist(),
        "options", getOptions());
  }

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    props.setImage(getImage());
    return props;
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_DatashieldContainerConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String name);

    public abstract Builder image(String image);

    public abstract Builder host(String host);

    public abstract Builder port(Integer port);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder versionId(@Nullable String versionId);

    public abstract Builder creationDate(@Nullable String creationDate);

    public abstract Builder autoUpdate(Boolean autoUpdate);

    public abstract Builder updateSchedule(@Nullable UpdateSchedule updateSchedule);

    public abstract Builder packageWhitelist(Set<String> packageWhitelist);

    public abstract Builder functionBlacklist(Set<String> functionBlacklist);

    public abstract Builder options(Map<String, String> options);

    @Nullable
    abstract String getImage();

    @Nullable
    abstract String getHost();

    @Nullable
    abstract Integer getPort();

    @Nullable
    abstract Boolean getAutoUpdate();

    @Nullable
    abstract Set<String> getPackageWhitelist();

    @Nullable
    abstract Set<String> getFunctionBlacklist();

    @Nullable
    abstract Map<String, String> getOptions();

    abstract DatashieldContainerConfig autoBuild();

    public DatashieldContainerConfig build() {
      if (getImage() == null) image("datashield/armadillo-rserver");
      if (getHost() == null) host("localhost");
      if (getPort() == null) port(6311);
      if (getAutoUpdate() == null) autoUpdate(false);
      if (getPackageWhitelist() == null) packageWhitelist(Set.of("dsBase"));
      if (getFunctionBlacklist() == null) functionBlacklist(Set.of());
      if (getOptions() == null) options(Map.of("datashield.seed", "342325352"));

      return autoBuild();
    }
  }
}
