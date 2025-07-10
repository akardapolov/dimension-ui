package ru.dimension.ui.model.db;

public enum DBType {
  ORACLE("oracle", "SELECT sysdate FROM dual"),
  POSTGRES("postgresql", "SELECT now()"),
  MSSQL("sqlserver", "SELECT getdate()"),
  CLICKHOUSE("clickhouse", "SELECT now()"),
  MYSQL("mysql", "SELECT now()"),
  DUCKDB("duckdb", "SELECT now()"),
  HTTP("https?://", ""),
  UNKNOWN("", "");

  private final String urlPattern;
  private final String query;

  DBType(String urlPattern,
         String query) {
    this.urlPattern = urlPattern;
    this.query = query;
  }

  public String getUrlPattern() {
    return this.urlPattern;
  }

  public String getQuery() {
    return query;
  }
}