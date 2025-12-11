package org.molgenis.armadillo.container;

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.Builder;
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
  public static DatashieldContainerConfig create(
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
    return builder()
        .name(newName)
        .image(newImage)
        .autoUpdate(autoUpdate)
        .updateSchedule(updateSchedule)
        .host(newHost != null ? newHost : "localhost")
        .port(newPort)
        .packageWhitelist(newPackageWhitelist != null ? newPackageWhitelist : Set.of())
        .functionBlacklist(newFunctionBlacklist != null ? newFunctionBlacklist : Set.of())
        .options(newOptions != null ? newOptions : Map.of())
        .lastImageId(newLastImageId)
        .versionId(newVersionId)
        .imageSize(newImageSize)
        .creationDate(newCreationDate)
        .installDate(newInstallDate)
        .build();
  }

  public static DatashieldContainerConfig createDefault() {
    // Start the build process
    return builder()
        .name("default")
        .image("datashield/armadillo-rserver")
        .autoUpdate(false)
        .host("localhost")
        .port(6311)
        .packageWhitelist(Set.of("dsBase"))
        .functionBlacklist(emptySet())
        .options(Map.of("datashield.seed", "342325352"))
        .build();
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_DatashieldContainerConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String name);

    public abstract Builder image(@Nullable String image);

    public abstract Builder host(String host);

    public abstract Builder port(Integer port);

    public abstract Builder lastImageId(@Nullable String lastImageId);

    public abstract Builder imageSize(@Nullable Long imageSize);

    public abstract Builder installDate(@Nullable String installDate);

    public abstract Builder autoUpdate(@Nullable Boolean autoUpdate);

    public abstract Builder updateSchedule(@Nullable UpdateSchedule updateSchedule);

    public abstract Builder packageWhitelist(Set<String> packageWhitelist);

    public abstract Builder functionBlacklist(Set<String> functionBlacklist);

    public abstract Builder options(Map<String, String> options);

    public abstract Builder versionId(@Nullable String versionId);

    public abstract Builder creationDate(@Nullable String creationDate);

    public abstract DatashieldContainerConfig build();
  }

  @Override
  public Map<String, Object> getSpecificContainerData() {

    Map<String, Object> specificData = new java.util.HashMap<>();

    specificData.put("autoUpdate", getAutoUpdate());
    specificData.put("updateSchedule", getUpdateSchedule());
    specificData.put("packageWhitelist", getPackageWhitelist());
    specificData.put("functionBlacklist", getFunctionBlacklist());
    specificData.put("options", getOptions());
    specificData.put("versionId", getVersionId());
    specificData.put("creationDate", getCreationDate());

    return specificData;
  }
}
