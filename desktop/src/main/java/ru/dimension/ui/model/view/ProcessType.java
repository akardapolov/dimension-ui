package ru.dimension.ui.model.view;

public enum ProcessType {
  REAL_TIME("Real-time"),
  HISTORY("History");

  private final String name;

  ProcessType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
