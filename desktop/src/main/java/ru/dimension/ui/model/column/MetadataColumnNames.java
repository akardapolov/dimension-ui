package ru.dimension.ui.model.column;

public enum MetadataColumnNames {
  COLUMN_ID("Column id"),
  COLUMN_ID_SQL("Column id SQL"),
  COLUMN("Name"),
  COLUMN_DB_TYPE_NAME("DB type"),
  STORAGE("Storage type"),
  COLUMN_TYPE("Local type"),
  TIMESTAMP("Timestamp"),
  DIMENSION("Dimension");

  private final String colName;

  MetadataColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
