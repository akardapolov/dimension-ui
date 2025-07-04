package ru.dimension.ui.model.parse;

public enum ParseType {
  JSON("Json"),
  PROMETHEUS("Prometheus");

  private final String name;

  ParseType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
