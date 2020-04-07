package org.molgenis.datashield.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@AutoValue
@JsonSerialize(as = DataShieldCommandDTO.class)
@JsonInclude(Include.NON_NULL)
public abstract class DataShieldCommandDTO {
  @JsonProperty
  public abstract Optional<Instant> startDate();

  @JsonProperty
  public abstract Instant createDate();

  @JsonProperty
  public abstract Optional<Instant> endDate();

  @JsonProperty
  public abstract UUID id();

  @JsonProperty
  public abstract String expression();

  @JsonProperty
  public abstract boolean withResult();

  @JsonProperty
  public abstract DataShieldCommand.DataShieldCommandStatus status();

  public static Builder builder() {
    return new AutoValue_DataShieldCommandDTO.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder startDate(Instant startDate);

    public abstract Builder createDate(Instant createDate);

    public abstract Builder endDate(Instant endDate);

    public abstract Builder id(UUID id);

    public abstract Builder expression(String expression);

    public abstract Builder withResult(boolean withResult);

    public abstract Builder status(DataShieldCommand.DataShieldCommandStatus status);

    public abstract DataShieldCommandDTO build();
  }
}
