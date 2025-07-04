package ru.dimension.ui.model.column;

public enum QueryColumnNames {
  ID("id"),
  NAME("Name"),
  FULL_NAME("Query name"),
  GATHER("Gather"),
  MODE("Mode"),
  DESCRIPTION("Description"),
  TEXT("Text"),
  PICK("Pick");

  private final String colName;

  QueryColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
