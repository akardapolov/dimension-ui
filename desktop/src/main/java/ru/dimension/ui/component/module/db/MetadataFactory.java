package ru.dimension.ui.component.module.db;

import ru.dimension.ui.model.db.DBType;

public class MetadataFactory {
  public static DatabaseMetadata create(DBType dbType) {
    return switch (dbType) {
      case ORACLE -> new OracleMetadata();
      case POSTGRES -> new PostgresMetadata();
      case MSSQL -> new MSSQLMetadata();
      case MYSQL -> new MySQLMetadata();
      case CLICKHOUSE -> new ClickHouseMetadata();
      case DUCKDB -> new DuckDBMetadata();
      default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
    };
  }
}
