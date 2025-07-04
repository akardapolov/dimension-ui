package ru.dimension.ui.model.column;

public enum QueryMetadataColumnNames {
  ID("id"),
  NAME("Name"),
  VALUE("Value"),
  DB_TYPE("DB type"),
  LOCAL_STORE("Store"),
  LOCAL_STORE_TYPE("Store type");

  private final String colName;

  QueryMetadataColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
