package ru.dimension.ui.model.view;

public enum AnalyzeType {
  REAL_TIME("Real-time"),
  HISTORY("History"),
  AD_HOC("Ad-hoc");

  private final String name;

  AnalyzeType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
