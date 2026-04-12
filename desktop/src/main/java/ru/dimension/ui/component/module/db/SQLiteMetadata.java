package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class SQLiteMetadata extends AbstractDatabaseMetadata {

  @Override
  public List<String> getSchemas(DatabaseMetaData metaData) throws SQLException {
    return Collections.emptyList();
  }

  @Override
  public List<String> getCatalogs(DatabaseMetaData metaData) throws SQLException {
    return Collections.emptyList();
  }

  @Override
  public ResultSet getTables(DatabaseMetaData metaData, String schema) throws SQLException {
    return metaData.getTables(null, null, null, new String[]{"TABLE"});
  }

  @Override
  public ResultSet getViews(DatabaseMetaData metaData, String schema) throws SQLException {
    return metaData.getTables(null, null, null, new String[]{"VIEW"});
  }

  @Override
  public ResultSet getOracleTables(Connection connection,
                                   String schema,
                                   String type) throws SQLException {
    return null;
  }
}