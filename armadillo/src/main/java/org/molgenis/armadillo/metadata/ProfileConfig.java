package org.molgenis.armadillo.metadata;

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
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

  @JsonProperty("host")
  @Nullable // defaults to localhost
  @NotEmpty
  public abstract String getHost();

  @JsonProperty("port")
  @Positive
  public abstract Integer getPort();

  @JsonProperty("username")
  @Nullable // applies to rock only
  public abstract String getUsername();

  @JsonProperty("password")
  @Nullable // applies to rock only
  public abstract String getPassword();

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
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("username") String newUsername,
      @JsonProperty("password") String newPassword,
      @JsonProperty("packageWhitelist") Set<String> newPackageWhitelist,
      @JsonProperty("functionBlacklist") Set<String> newFunctionBlacklist,
      @JsonProperty("options") Map<String, String> newOptions) {
    return new AutoValue_ProfileConfig(
        newName,
        newImage,
        newHost != null ? newHost : "localhost",
        newPort,
        newUsername,
        newPassword,
        newPackageWhitelist,
        newFunctionBlacklist,
        newOptions != null ? newOptions : Map.of());
  }

  @JsonCreator
  public static ProfileConfig createDefault() {
    return create(
        "default",
        "datashield/armadillo-rserver",
        "localhost",
        6311,
        null,
        null,
        Set.of("dsBase"),
        emptySet(),
        Map.of("datashield.seed", "342325352"));
  }

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    props.setUsername(getUsername());
    props.setPassword(getPassword());
    return props;
  }
}
