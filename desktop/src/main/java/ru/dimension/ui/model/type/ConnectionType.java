package ru.dimension.ui.model.type;

public enum ConnectionType {
  JDBC("JDBC"),
  HTTP("HTTP");

  private final String name;

  ConnectionType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
