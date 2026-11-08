package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

public class OracleMetadata extends AbstractDatabaseMetadata {
  private static final Set<String> EXCLUDED_SCHEMAS = Set.of("MDSYS");

  @Override
  public Set<String> getExcludedSchemas() {
    return EXCLUDED_SCHEMAS;
  }

  @Override
  public ResultSet getOracleTables(Connection connection, String schema, String type) throws SQLException {
    if (schema == null || schema.isBlank()) {
      throw new SQLException("Schema is not selected");
    }

    String sql = "SELECT o.object_name AS table_name " +
        "FROM all_objects o " +
        "WHERE o.owner = ? " +
        "  AND o.object_type = ? " +
        "ORDER BY table_name";

    PreparedStatement statement = connection.prepareStatement(sql);
    try {
      statement.closeOnCompletion();
      statement.setString(1, schema);
      statement.setString(2, type.toUpperCase(Locale.ROOT));
      return statement.executeQuery();
    } catch (SQLException | RuntimeException e) {
      try {
        statement.close();
      } catch (SQLException closeEx) {
        e.addSuppressed(closeEx);
      }
      throw e;
    }
  }
}