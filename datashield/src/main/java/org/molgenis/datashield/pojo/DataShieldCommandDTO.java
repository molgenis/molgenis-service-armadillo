package org.molgenis.datashield.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.rosuda.REngine.REXP;

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

  public static DataShieldCommandDTO create(DataShieldCommand<REXP> command) {
    Builder builder =
        builder()
            .createDate(command.getCreateDate())
            .expression(command.getExpression())
            .id(command.getId())
            .status(command.getStatus())
            .withResult(command.isWithResult());
    command.getStartDate().ifPresent(builder::startDate);
    command.getEndDate().ifPresent(builder::endDate);
    return builder.build();
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

    abstract DataShieldCommandDTO build();
  }
}
