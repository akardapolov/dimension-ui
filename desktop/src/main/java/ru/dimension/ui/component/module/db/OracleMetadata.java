package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class OracleMetadata extends AbstractDatabaseMetadata {
  private static final Set<String> EXCLUDED_SCHEMAS = Set.of("MDSYS");

  @Override
  public Set<String> getExcludedSchemas() {
    return EXCLUDED_SCHEMAS;
  }

  @Override
  public ResultSet getOracleTables(Connection connection, String schema, String type) throws SQLException {
    String sql = "SELECT o.object_name AS table_name " +
        "FROM all_objects o " +
        "WHERE o.owner = ? " +
        "  AND o.object_type = ? " +
        "ORDER BY table_name";
    PreparedStatement statement = connection.prepareStatement(sql);
    statement.setString(1, schema.toUpperCase());
    statement.setString(2, type.toUpperCase());
    return statement.executeQuery();
  }
}