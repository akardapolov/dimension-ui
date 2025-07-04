package ru.dimension.ui.model.view;

public enum ConfigState {
  SHOW("Show"),
  HIDE("Hide");

  private final String description;

  ConfigState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }
}
