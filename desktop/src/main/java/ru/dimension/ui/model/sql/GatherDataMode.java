package ru.dimension.ui.model.sql;

public enum GatherDataMode {
  BY_CLIENT_JDBC("by_client"),
  BY_SERVER_JDBC("by_server"),
  BY_CLIENT_HTTP("by_client_http");

  private final String description;

  GatherDataMode(String description) {
    this.description = description;
  }
}
