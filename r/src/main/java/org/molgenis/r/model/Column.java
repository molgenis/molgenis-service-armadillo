package org.molgenis.r.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

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

    abstract Column autoBuild();

    public Column build() {
      Column column = autoBuild();
      Preconditions.checkState(
          column.name().matches("[\\w#]+(-[a-z]{2,3})??$"), "Invalid column name");
      return column;
    }
  }
}
