package ru.dimension.ui.model.column;

public enum ColumnNames {
  ID("id"),
  NAME("Name"),
  PICK("Pick");

  private final String colName;

  ColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
