package ru.dimension.ui.model.view;

public enum ProgressbarState {
  SHOW("Show"),
  HIDE("Hide");

  private final String description;

  ProgressbarState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
