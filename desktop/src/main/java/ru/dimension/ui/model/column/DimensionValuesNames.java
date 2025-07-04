package ru.dimension.ui.model.column;

public enum DimensionValuesNames {
  COLOR("Color"),
  VALUE("Value");

  private final String colName;

  DimensionValuesNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
