package ru.dimension.ui.model.view.tab;

public enum ReportTabPane {
  DESIGN("Design"),
  REPORT("Report");

  private final String name;

  ReportTabPane(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
