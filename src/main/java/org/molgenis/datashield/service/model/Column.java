package org.molgenis.datashield.service.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Column {
  public abstract String name();
  public abstract ColumnType type();

  public static Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);
    public abstract Builder setType(ColumnType type);
    public abstract Column build();
  }
}
