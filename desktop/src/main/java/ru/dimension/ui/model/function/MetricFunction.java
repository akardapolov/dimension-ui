package ru.dimension.ui.model.function;

public enum MetricFunction {
  NONE("None"),
  COUNT("Count"),
  SUM("Sum"),
  AVG("Avg");

  private final String name;

  MetricFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
