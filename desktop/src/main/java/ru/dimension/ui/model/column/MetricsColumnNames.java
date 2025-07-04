package ru.dimension.ui.model.column;

public enum MetricsColumnNames {
  ID("id"),
  NAME("Name"),
  COLUMN_NAME("Column name"),
  METRIC_NAME("Metric name"),
  PICK("Pick"),
  IS_DEFAULT("Default"),
  X_AXIS("X Axis"),
  Y_AXIS("Y Axis"),
  GROUP("Group"),
  METRIC_FUNCTION("Metric function"),
  CHART_TYPE("Chart type");


  private final String colName;

  MetricsColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
