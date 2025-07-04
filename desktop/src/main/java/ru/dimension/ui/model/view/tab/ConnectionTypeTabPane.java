package ru.dimension.ui.model.view.tab;

public enum ConnectionTypeTabPane {
  JDBC("JDBC"),
  HTTP("HTTP");

  private final String name;

  ConnectionTypeTabPane(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
