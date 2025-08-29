package ru.dimension.ui.model.function;

public enum TimeRangeFunction {
  AUTO("Auto"),
  MINUTE("Minute"),
  HOUR("Hour"),
  DAY("Day"),
  MONTH("Month");

  private final String name;

  TimeRangeFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}