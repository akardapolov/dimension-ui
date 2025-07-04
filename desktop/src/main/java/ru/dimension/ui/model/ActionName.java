package ru.dimension.ui.model;

public enum ActionName {
  START("Start"),
  STOP("Stop");

  private final String description;

  ActionName(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }
}
