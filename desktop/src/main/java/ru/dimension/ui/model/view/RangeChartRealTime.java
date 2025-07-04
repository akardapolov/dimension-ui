package ru.dimension.ui.model.view;

public enum RangeChartRealTime {
  FIVE_MIN("5 min"),
  TEN_MIN("10 min"),
  THIRTY_MIN("30 min"),
  SIXTY_MIN("60 min");

  private final String name;

  RangeChartRealTime(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
