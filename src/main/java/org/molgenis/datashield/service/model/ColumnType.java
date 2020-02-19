package org.molgenis.datashield.service.model;

public enum ColumnType {
  BOOL("bool"),
  DATE("date"),
  DATE_TIME("date_time"),
  DECIMAL("decimal"),
  INT("int"),
  LONG("long"),
  STRING("string");

  private String name;

  ColumnType(String name) {
    this.name = name;
  }
}
