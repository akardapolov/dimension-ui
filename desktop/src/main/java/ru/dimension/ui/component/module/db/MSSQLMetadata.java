package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class MSSQLMetadata extends AbstractDatabaseMetadata {
  private static final Set<String> EXCLUDED_SCHEMAS = Set.of(
      "db_accessadmin", "db_backupoperator", "db_datareader", "db_datawriter",
      "db_ddladmin", "db_denydatareader", "db_denydatawriter", "db_owner",
      "db_securityadmin", "guest"
  );

  @Override
  public ResultSet getOracleTables(Connection connection,
                                   String schema,
                                   String type) throws SQLException {
    return null;
  }

  @Override
  public Set<String> getExcludedSchemas() {
    return EXCLUDED_SCHEMAS;
  }
}