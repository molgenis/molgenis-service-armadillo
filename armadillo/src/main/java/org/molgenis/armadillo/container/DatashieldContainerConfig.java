package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.UpdateSchedule;
import org.molgenis.r.config.EnvironmentConfigProps;

@AutoValue
@JsonTypeName("ds")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DatashieldContainerConfig
    implements ContainerConfig, UpdatableContainer, OpenContainer {

  @Override
  @Nullable
  public abstract String getName();

  @Override
  @Nullable
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

  @Nullable
  @JsonIgnore
  public abstract Set<String> getPackageWhitelist();

  @Nullable
  @JsonIgnore
  public abstract Set<String> getFunctionBlacklist();

  @Nullable
  @JsonIgnore
  public abstract Map<String, String> getOptions();

  @Override
  @JsonIgnore
  public Map<String, Object> getSpecificContainerConfig() {
    return Map.of(
        "packageWhitelist", getPackageWhitelist(),
        "functionBlacklist", getFunctionBlacklist(),
        "options", getOptions());
  }

  @Override
  @JsonIgnore
  public String getType() {
    return "ds";
  }

  @JsonCreator
  public static DatashieldContainerConfig create(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("image") @Nullable String image,
      @JsonProperty("host") @Nullable String host,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("lastImageId") @Nullable String lastImageId,
      @JsonProperty("imageSize") @Nullable Long imageSize,
      @JsonProperty("installDate") @Nullable String installDate,
      @JsonProperty("versionId") @Nullable String versionId,
      @JsonProperty("creationDate") @Nullable String creationDate,
      @JsonProperty("autoUpdate") @Nullable Boolean autoUpdate,
      @JsonProperty("updateSchedule") @Nullable UpdateSchedule updateSchedule,
      @JsonProperty("packageWhitelist") @Nullable Set<String> packageWhitelist,
      @JsonProperty("functionBlacklist") @Nullable Set<String> functionBlacklist,
      @JsonProperty("options") @Nullable Map<String, String> options) {

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

  public static DatashieldContainerConfig createDefault() {
    return builder().name("default").build();
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
    public abstract Builder name(@Nullable String name);

    public abstract Builder image(@Nullable String image);

    public abstract Builder host(@Nullable String host);

    public abstract Builder port(@Nullable Integer port);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder versionId(@Nullable String versionId);

    public abstract Builder creationDate(@Nullable String creationDate);

    public abstract Builder autoUpdate(@Nullable Boolean autoUpdate);

    public abstract Builder updateSchedule(@Nullable UpdateSchedule updateSchedule);

    public abstract Builder packageWhitelist(@Nullable Set<String> packageWhitelist);

    public abstract Builder functionBlacklist(@Nullable Set<String> functionBlacklist);

    public abstract Builder options(@Nullable Map<String, String> options);

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
