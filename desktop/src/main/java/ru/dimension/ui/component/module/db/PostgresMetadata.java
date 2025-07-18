package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresMetadata extends AbstractDatabaseMetadata {

  @Override
  public ResultSet getOracleTables(Connection connection,
                                   String schema,
                                   String type) throws SQLException {
    return null;
  }
}
