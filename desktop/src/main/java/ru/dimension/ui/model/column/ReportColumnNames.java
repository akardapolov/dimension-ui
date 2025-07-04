package ru.dimension.ui.model.column;

public enum ReportColumnNames {
  ID("id"),
  REPORT_NAME("Report name");

  private final String colName;

  ReportColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
