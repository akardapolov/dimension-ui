package ru.dimension.ui.component.module.db;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClickHouseMetadata extends AbstractDatabaseMetadata {

  @Override
  public ResultSet getTables(DatabaseMetaData metaData, String catalog) throws SQLException {
    return metaData.getTables(catalog, null, null, new String[]{"TABLE"});
  }

  @Override
  public ResultSet getViews(DatabaseMetaData metaData, String catalog) throws SQLException {
    return metaData.getTables(catalog, null, null, new String[]{"VIEW"});
  }

  @Override
  public ResultSet getOracleTables(Connection connection,
                                   String schema,
                                   String type) throws SQLException {
    return null;
  }
}
