package org.molgenis.armadillo.metadata;

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.Set;
import org.molgenis.r.config.EnvironmentConfigProps;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProfileConfig {
  @JsonProperty("name")
  @NotEmpty
  public abstract String getName();

  @JsonProperty("image")
  @Nullable // only required when docker enabled
  public abstract String getImage();

  @JsonProperty("autoUpdate")
  @Nullable
  public abstract Boolean getAutoUpdate();

  @JsonProperty("autoUpdateSchedule")
  @Nullable
  public abstract AutoUpdateSchedule getAutoUpdateSchedule();

  @JsonProperty("host")
  @Nullable // defaults to localhost
  @NotEmpty
  public abstract String getHost();

  @JsonProperty("port")
  @Positive
  public abstract Integer getPort();

  @JsonProperty("packageWhitelist")
  public abstract Set<String> getPackageWhitelist();

  @JsonProperty("functionBlacklist")
  public abstract Set<String> getFunctionBlacklist();

  @JsonProperty("options")
  public abstract Map<String, String> getOptions();

  @JsonCreator
  public static ProfileConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("autoUpdate") Boolean autoUpdate,
      @JsonProperty("autoUpdateSchedule") AutoUpdateSchedule autoUpdateSchedule,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("packageWhitelist") Set<String> newPackageWhitelist,
      @JsonProperty("functionBlacklist") Set<String> newFunctionBlacklist,
      @JsonProperty("options") Map<String, String> newOptions) {
    return new AutoValue_ProfileConfig(
        newName,
        newImage,
        autoUpdate,
        autoUpdateSchedule,
        newHost != null ? newHost : "localhost",
        newPort,
        newPackageWhitelist,
        newFunctionBlacklist,
        newOptions != null ? newOptions : Map.of());
  }

  private static final AutoUpdateSchedule DEFAULT_SCHEDULE =
      new AutoUpdateSchedule("weekly", "Sunday", "03:00");

  public static ProfileConfig createDefault() {
    return create(
        "default",
        "datashield/armadillo-rserver",
        false,
        DEFAULT_SCHEDULE,
        "localhost",
        6311,
        Set.of("dsBase"),
        emptySet(),
        Map.of("datashield.seed", "342325352"));
  }

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    props.setImage(getImage());
    return props;
  }
}
