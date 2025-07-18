package ru.dimension.ui.component.module.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DatabaseMetadata {
  List<String> getSchemas(DatabaseMetaData metaData) throws SQLException;
  List<String> getCatalogs(DatabaseMetaData metaData) throws SQLException;
  ResultSet getTables(DatabaseMetaData metaData, String schema) throws SQLException;
  ResultSet getViews(DatabaseMetaData metaData, String schema) throws SQLException;
  ResultSet getOracleTables(Connection connection, String schema, String type) throws SQLException;
  Set<String> getExcludedSchemas();
  Set<String> getExcludedCatalogs();
}