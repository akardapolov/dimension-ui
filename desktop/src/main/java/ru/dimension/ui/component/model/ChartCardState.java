package ru.dimension.ui.component.model;

public enum ChartCardState {
  COLLAPSE_ALL("Collapse all"),
  EXPAND_ALL("Expand all");

  private final String name;

  ChartCardState(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public ChartCardState toggle() {
    return this == COLLAPSE_ALL ? EXPAND_ALL : COLLAPSE_ALL;
  }
}
