package ru.dimension.ui.component.model;

public enum AnalyzeTabType {
  ANOMALY("Anomaly"),
  FORECAST("Forecast");

  private final String name;

  AnalyzeTabType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
