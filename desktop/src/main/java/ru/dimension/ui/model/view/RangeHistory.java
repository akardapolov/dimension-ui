package ru.dimension.ui.model.view;

public enum RangeHistory {
  DAY("Day"),
  WEEK("Week"),
  MONTH("Month"),
  CUSTOM("Custom");

  private final String name;

  RangeHistory(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
