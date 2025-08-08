package org.molgenis.armadillo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import java.util.*;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ArmadilloColumnMetaData {
  private int missing = 0;
  private long total = 0;
  private Set<String> possibleLevels = new HashSet<>();

  @JsonProperty("type")
  @NotEmpty
  public abstract String getType();

  @JsonProperty("missing")
  @NotEmpty
  public String getTotalMissing() {
    return getMissing() + "/" + getTotal();
  }

  @JsonProperty("levels")
  @Nullable
  public Set<String> getLevels() {
    if (Objects.equals(getType(), "BINARY")) {
      return getPossibleLevels();
    }
    return null;
  }

  @JsonCreator
  public static ArmadilloColumnMetaData create(@JsonProperty("type") String newType) {
    return new AutoValue_ArmadilloColumnMetaData(newType);
  }

  public void countMissingValue() {
    missing += 1;
  }

  private long getTotal() {
    return total;
  }

  private int getMissing() {
    return missing;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public void setMissing(int missing) {
    this.missing = missing;
  }

  public void setPossibleLevels(Set<String> possibleLevels) {
    this.possibleLevels = possibleLevels;
  }

  private Set<String> getPossibleLevels() {
    return possibleLevels;
  }
}
