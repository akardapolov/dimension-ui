package ru.dimension.ui.model.function;

public enum PercentileFunction {
  NONE("None"),
  P50("P50"),
  P90("P90"),
  P95("P95"),
  P99("P99");

  private final String name;

  PercentileFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
