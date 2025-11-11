package ru.dimension.ui.model.column;

public enum SeriesNames {
  COLOR("Color"),
  SERIES("Series"),
  PICK("Pick");

  private final String colName;

  SeriesNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
