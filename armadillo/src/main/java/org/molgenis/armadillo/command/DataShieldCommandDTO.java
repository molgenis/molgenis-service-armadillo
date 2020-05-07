package org.molgenis.armadillo.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.molgenis.armadillo.command.Commands.DataShieldCommandStatus;

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
  @Nullable
  public abstract String message();

  @JsonProperty
  public abstract DataShieldCommandStatus status();

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

    public abstract Builder status(DataShieldCommandStatus status);

    public abstract Builder message(String message);

    public abstract DataShieldCommandDTO build();
  }
}
