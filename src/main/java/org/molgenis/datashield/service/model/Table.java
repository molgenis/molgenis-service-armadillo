package org.molgenis.datashield.service.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class Table {
  public abstract String name();

  public abstract ImmutableList<Column> columns();

  public static Builder builder() {
    return new AutoValue_Table.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    abstract ImmutableList.Builder<Column> columnsBuilder();

    public Builder addColumn(Column column) {
      columnsBuilder().add(column);
      return this;
    }

    public abstract Table build();
  }
}
