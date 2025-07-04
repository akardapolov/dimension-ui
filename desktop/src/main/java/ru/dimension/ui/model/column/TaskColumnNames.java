package ru.dimension.ui.model.column;

public enum TaskColumnNames {
  ID("id"),
  NAME("Name"),
  PULL_TIMEOUT("Pull timeout");


  private final String colName;

  TaskColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
