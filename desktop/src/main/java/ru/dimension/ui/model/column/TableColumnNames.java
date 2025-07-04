package ru.dimension.ui.model.column;

public enum TableColumnNames {
  TABLE_NAME("Name"),
  TABLE_TYPE("Type"),
  INDEX_TYPE("Index type"),
  COMPRESSION("Compression");


  private final String colName;

  TableColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
