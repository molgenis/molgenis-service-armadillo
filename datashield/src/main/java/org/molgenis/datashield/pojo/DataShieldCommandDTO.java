package org.molgenis.datashield.pojo;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.UUID;
import org.rosuda.REngine.REXP;

@AutoValue
public abstract class DataShieldCommandDTO {
  public abstract Instant startDate();

  public abstract Instant createDate();

  public abstract Instant endDate();

  public abstract UUID id();

  public abstract String expression();

  public abstract boolean withResult();

  public abstract DataShieldCommandStatus status();

  public static Builder builder() {
    return new AutoValue_DataShieldCommandDTO.Builder();
  }

  public static DataShieldCommandDTO create(DataShieldCommand<REXP> datashieldCommand) {
    return null;
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

    abstract DataShieldCommandDTO build();
  }
}
