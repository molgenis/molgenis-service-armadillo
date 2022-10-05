package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.molgenis.r.config.EnvironmentConfigProps;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProfileConfig {
  @JsonProperty("name")
  public abstract String getName();

  @JsonProperty("image")
  @Nullable // only required when docker enabled
  public abstract String getImage();

  @JsonProperty("host")
  @Nullable // defaults to localhost
  public abstract String getHost();

  @JsonProperty("port")
  public abstract Integer getPort();

  @JsonProperty("whitelist")
  public abstract Set<String> getWhitelist();

  @JsonProperty("options")
  public abstract Map<String, String> getOptions();

  @JsonCreator
  public static ProfileConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("whitelist") Set<String> newWhitelist,
      @JsonProperty("options") Map<String, String> newOptions) {
    return new AutoValue_ProfileConfig(
        newName,
        newImage,
        newHost != null ? newHost : "localhost",
        newPort,
        newWhitelist,
        newOptions != null ? newOptions : Map.of());
  }

  public EnvironmentConfigProps toEnvironmentConfigProps() {
    var props = new EnvironmentConfigProps();
    props.setName(getName());
    props.setHost(getHost());
    props.setPort(getPort());
    return props;
  }
}
