package ru.dimension.ui.model.view;

public enum ToolbarButtonState {
  ENABLE("Enable"),
  DISABLE("Disable");

  private final String description;

  ToolbarButtonState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
