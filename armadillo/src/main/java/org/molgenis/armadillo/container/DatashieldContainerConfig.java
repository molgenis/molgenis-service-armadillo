package org.molgenis.armadillo.container;

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.metadata.UpdateSchedule;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DatashieldContainerConfig extends AbstractContainerConfig {

  @JsonProperty("autoUpdate")
  @Nullable
  public abstract Boolean getAutoUpdate();

  @JsonProperty("updateSchedule")
  @Nullable
  public abstract UpdateSchedule getUpdateSchedule();

  @JsonProperty("packageWhitelist")
  public abstract Set<String> getPackageWhitelist();

  @JsonProperty("functionBlacklist")
  public abstract Set<String> getFunctionBlacklist();

  @JsonProperty("options")
  public abstract Map<String, String> getOptions();

  @JsonProperty("versionId")
  @Nullable
  public abstract String getVersionId();

  @JsonProperty("CreationDate")
  @Nullable
  public abstract String getCreationDate();

  @JsonCreator
  public static ContainerConfig create(
      @JsonProperty("name") String newName,
      @JsonProperty("image") String newImage,
      @JsonProperty("autoUpdate") Boolean autoUpdate,
      @JsonProperty("updateSchedule") UpdateSchedule updateSchedule,
      @JsonProperty("host") String newHost,
      @JsonProperty("port") Integer newPort,
      @JsonProperty("packageWhitelist") Set<String> newPackageWhitelist,
      @JsonProperty("functionBlacklist") Set<String> newFunctionBlacklist,
      @JsonProperty("options") Map<String, String> newOptions,
      @JsonProperty("lastImageId") @Nullable String newLastImageId,
      @JsonProperty("versionId") @Nullable String newVersionId,
      @JsonProperty("imageSize") @Nullable Long newImageSize,
      @JsonProperty("creationDate") @Nullable String newCreationDate,
      @JsonProperty("installDate") @Nullable String newInstallDate) {
    return new AutoValue_DatashieldContainerConfig(
        newName,
        newImage,
        autoUpdate,
        updateSchedule,
        newHost != null ? newHost : "localhost",
        newPort,
        newPackageWhitelist != null ? newPackageWhitelist : Set.of(),
        newFunctionBlacklist != null ? newFunctionBlacklist : Set.of(),
        newOptions != null ? newOptions : Map.of(),
        newLastImageId,
        newVersionId,
        newImageSize,
        newCreationDate,
        newInstallDate);
  }

  public static ContainerConfig createDefault() {
    return create(
        "default",
        "datashield/armadillo-rserver",
        false,
        null,
        "localhost",
        6311,
        Set.of("dsBase"),
        emptySet(),
        Map.of("datashield.seed", "342325352"),
        null,
        null,
        null,
        null,
        null);
  }
}
