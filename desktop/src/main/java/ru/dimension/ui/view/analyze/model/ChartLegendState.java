package ru.dimension.ui.view.analyze.model;

public enum ChartLegendState {
  SHOW("Show"),
  HIDE("Hide");

  private final String name;

  ChartLegendState(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public ChartLegendState toggle() {
    return this == SHOW ? HIDE : SHOW;
  }
}