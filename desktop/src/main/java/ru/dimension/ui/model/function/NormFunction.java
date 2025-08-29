package ru.dimension.ui.model.function;

public enum NormFunction {
  NONE("None"),
  SECOND("Second"),
  MINUTE("Minute"),
  HOUR("Hour"),
  DAY("Day");

  private final String name;

  NormFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}