package org.molgenis.datashield.service.model;

public enum ColumnType {
  LOGICAL("logical"),
  DATE("date"),
  DATE_TIME("date_time"),
  DOUBLE("double"),
  INTEGER("integer"),
  CHARACTER("character"),
  FACTOR("factor");

  private String name;

  ColumnType(String name) {
    this.name = name;
  }
}
