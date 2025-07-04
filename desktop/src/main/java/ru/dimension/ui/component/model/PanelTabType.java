package ru.dimension.ui.component.model;

public enum PanelTabType {
  REALTIME("Real-time"),
  HISTORY("History");

  private final String name;

  PanelTabType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
