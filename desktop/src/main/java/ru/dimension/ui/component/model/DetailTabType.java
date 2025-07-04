package ru.dimension.ui.component.model;

public enum DetailTabType {
  TOP("Top"),
  PIVOT("Pivot"),
  RAW("Raw");

  private final String name;

  DetailTabType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
