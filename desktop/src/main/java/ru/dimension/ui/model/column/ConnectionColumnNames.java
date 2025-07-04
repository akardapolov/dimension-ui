package ru.dimension.ui.model.column;

public enum ConnectionColumnNames {
  ID("id"),
  NAME("Name"),
  USER_NAME("User name"),
  PASSWORD("password"),
  URL("URL"),
  JAR("jar"),
  DRIVER("driver"),
  TYPE("Type"),
  HTTP_METHOD("Method"),
  HTTP_PARSE_TYPE("Parse"),
  CONNECTION_JDBC("Connection JDBC");

  private final String colName;

  ConnectionColumnNames(String colName) {
    this.colName = colName;
  }

  public String getColName() {
    return this.colName;
  }
}
