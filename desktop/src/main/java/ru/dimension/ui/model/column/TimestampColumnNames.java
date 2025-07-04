package ru.dimension.ui.model.column;

public enum TimestampColumnNames {
  ID("id"),
  NAME("Name");

  private final String colName;

  TimestampColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
