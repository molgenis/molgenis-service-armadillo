package org.molgenis.datashield.service.model;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Character.isLetter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonSerialize(as = Package.class)
public abstract class Package {

  @JsonProperty("name")
  public abstract String name();

  @JsonProperty("libPath")
  public abstract String libPath();

  @JsonProperty("version")
  public abstract String version();

  @JsonProperty("built")
  public abstract String built();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setLibPath(String libPath);

    public abstract Builder setBuilt(String built);

    public abstract Builder setVersion(String version);

    abstract Package autoBuild();

    public Package build() {
      Package pack = autoBuild();
      checkName(pack.name());
      return pack;
    }
  }

  static void checkName(String name) {
    checkState(
        name.matches("^[a-zA-Z0-9.]+$"),
        "Invalid package name: '%s'. Package name can only consist of letters, numbers and periods",
        name);
    checkState(
        isLetter(name.charAt(0)),
        "Invalid package name: '%s'. Package name must start with a letter",
        name);
    checkState(
        name.charAt(name.length() - 1) != '.',
        "Invalid package name: '%s'. Package name cannot end with a period",
        name);
  }

  public static Builder builder() {
    return new AutoValue_Package.Builder();
  }
}
