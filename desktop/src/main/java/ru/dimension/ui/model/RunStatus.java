package ru.dimension.ui.model;

public enum RunStatus {
  RUNNING("Running"),
  NOT_RUNNING("Not running");

  private final String description;

  RunStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }
}
