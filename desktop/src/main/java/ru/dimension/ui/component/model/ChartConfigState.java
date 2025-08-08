package ru.dimension.ui.component.model;

public enum ChartConfigState {
  SHOW("Show"),
  HIDE("Hide");

  private final String name;

  ChartConfigState(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public ChartConfigState toggle() {
    return this == SHOW ? HIDE : SHOW;
  }
}