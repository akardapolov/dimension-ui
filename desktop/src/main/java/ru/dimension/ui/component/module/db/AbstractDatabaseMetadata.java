package ru.dimension.ui.component.module.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractDatabaseMetadata implements DatabaseMetadata {
  @Override
  public List<String> getSchemas(DatabaseMetaData metaData) throws SQLException {
    List<String> list = new ArrayList<>();
    try (ResultSet schemas = metaData.getSchemas()) {
      while (schemas.next()) {
        String schema = schemas.getString(1);
        if (!getExcludedSchemas().contains(schema)) {
          list.add(schema);
        }
      }
    }
    return list;
  }

  @Override
  public List<String> getCatalogs(DatabaseMetaData metaData) throws SQLException {
    List<String> list = new ArrayList<>();
    try (ResultSet catalogs = metaData.getCatalogs()) {
      while (catalogs.next()) {
        String catalog = catalogs.getString(1);
        if (!getExcludedCatalogs().contains(catalog)) {
          list.add(catalog);
        }
      }
    }
    return list;
  }

  @Override
  public ResultSet getTables(DatabaseMetaData metaData, String schema) throws SQLException {
    return metaData.getTables(null, schema, null, new String[]{"TABLE"});
  }

  @Override
  public ResultSet getViews(DatabaseMetaData metaData, String schema) throws SQLException {
    return metaData.getTables(null, schema, null, new String[]{"VIEW"});
  }

  @Override
  public Set<String> getExcludedSchemas() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getExcludedCatalogs() {
    return Collections.emptySet();
  }
}
