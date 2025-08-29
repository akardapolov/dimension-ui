package ru.dimension.ui.model.chart;

public enum ChartType {
  NONE("None"),
  LINEAR("Linear"),
  STACKED("Stacked");

  private final String description;

  ChartType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
