package ru.dimension.ui.model.view;

public enum ReportState {
  SHOW("Show"),
  HIDE("Hide");

  private final String description;

  ReportState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
