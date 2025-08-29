package ru.dimension.ui.model.function;

public enum GroupFunction {
  NONE("None"),
  COUNT("Count"),
  SUM("Sum"),
  AVG("Avg");

  private final String name;

  GroupFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
