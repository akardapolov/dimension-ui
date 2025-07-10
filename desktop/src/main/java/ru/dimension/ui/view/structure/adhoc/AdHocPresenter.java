package ru.dimension.ui.view.structure.adhoc;

import static ru.dimension.ui.model.db.DBType.CLICKHOUSE;
import static ru.dimension.ui.model.db.DBType.DUCKDB;
import static ru.dimension.ui.model.db.DBType.MSSQL;
import static ru.dimension.ui.model.db.DBType.MYSQL;
import static ru.dimension.ui.model.db.DBType.ORACLE;
import static ru.dimension.ui.model.db.DBType.POSTGRES;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.AdHocListener;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.panel.adhoc.AdHocPanel;
import ru.dimension.ui.view.structure.AdHocView;

@Log4j2
@Singleton
public class AdHocPresenter implements AdHocListener {

  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final EventListener eventListener;
  private final AdHocView adHocView;
  private final JXTableCase connectionCase;
  private final JXTitledSeparator tableNameAdHocTitle;
  private final JXTableCase timestampCase;
  private final JXTableCase metricCase;
  private final JXTableCase columnCase;
  private final JComboBox<String> schemaCatalogCBox;
  private final JXTableCase tableCase;
  private final JXTableCase viewCase;
  private final ConnectionPoolManager connectionPoolManager;
  private final AdHocDatabaseManager adHocDatabaseManager;
  private java.sql.Connection connection;
  private final AdHocPanel adHocPanel;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private String connectionName = "";
  private Thread periodicTask;

  private final Set<String> excludedSchemasMSSQL = Set.of(
      "db_accessadmin",
      "db_backupoperator",
      "db_datareader",
      "db_datawriter",
      "db_ddladmin",
      "db_denydatareader",
      "db_denydatawriter",
      "db_owner",
      "db_securityadmin",
      "guest"
  );

  private final Set<String> excludedSchemasOracle = Set.of(
      "MDSYS"
  );

  private final Set<String> excludedSchemasMySQL = Set.of(
      "information_schema",
      "mysql",
      "performance_schema",
      "sys"
  );

  @Inject
  public AdHocPresenter(@Named("profileManager") ProfileManager profileManager,
                        @Named("configurationManager") ConfigurationManager configurationManager,
                        @Named("eventListener") EventListener eventListener,
                        @Named("adHocView") AdHocView adHocView,
                        @Named("connectionAdHocCase") JXTableCase connectionCase,
                        @Named("schemaCatalogAdHocCBox") JComboBox<String> schemaCatalogCBox,
                        @Named("tableAdHocCase") JXTableCase tableCase,
                        @Named("viewAdHocCase") JXTableCase viewCase,
                        @Named("tableNameAdHocTitle") JXTitledSeparator tableNameAdHocTitle,
                        @Named("timestampAdHocCase") JXTableCase timestampCase,
                        @Named("metricAdHocCase") JXTableCase metricCase,
                        @Named("columnAdHocCase") JXTableCase columnCase,
                        @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager,
                        @Named("adHocDatabaseManager") AdHocDatabaseManager adHocDatabaseManager,
                        @Named("adHocPanel") AdHocPanel adHocPanel) {
    this.profileManager = profileManager;
    this.eventListener = eventListener;
    this.eventListener.addAdHocStateListener(this);
    this.configurationManager = configurationManager;
    this.connectionPoolManager = connectionPoolManager;
    this.adHocDatabaseManager = adHocDatabaseManager;

    this.adHocView = adHocView;

    this.adHocPanel = adHocPanel;

    this.connectionCase = connectionCase;
    this.schemaCatalogCBox = schemaCatalogCBox;
    this.tableCase = tableCase;
    this.viewCase = viewCase;
    this.tableNameAdHocTitle = tableNameAdHocTitle;
    this.timestampCase = timestampCase;
    this.metricCase = metricCase;
    this.columnCase = columnCase;

    Consumer<String> runAction = this::runAction;
    new TableSelectionHandler(this.connectionCase, ConnectionColumnNames.NAME.getColName(), runAction);

    this.schemaCatalogCBox.addActionListener(e -> reloadTables());
  }

  private void runAction(String name) {
    if (running.get()) {
      int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                "Loading metadata for connection " + connectionName
                                                    + ". Do you want to interrupt loading?");

      if (input == 0) {
        running.set(false);
        periodicTask.interrupt();
      }

      return;
    }

    running.set(true);

    adHocPanel.setClearFlag(true);

    adHocPanel.getDetailsControlPanelHandler().clearAll();

    adHocPanel.cleanVisualizeAndAnalyze();

    columnCase.removeAllElements();
    metricCase.removeAllElements();
    timestampCase.removeAllElements();

    viewCase.setBlockRunAction(true);
    tableCase.setBlockRunAction(true);
    viewCase.removeAllElements();
    tableCase.removeAllElements();
    viewCase.setBlockRunAction(false);
    tableCase.setBlockRunAction(false);

    schemaCatalogCBox.removeAllItems();

    tableNameAdHocTitle.setTitle("Table name: not selected");

    adHocPanel.setClearFlag(false);

    connectionName = GUIHelper.getNameByColumnName(connectionCase,
                                                   connectionCase.getJxTable().getSelectionModel(),
                                                   ConnectionColumnNames.NAME.getColName());

    periodicTask = Thread.ofVirtual().start(() -> {
      while (running.get()) {
        loadMetadata(GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName()));
      }
    });
  }

  private void reloadTables() {
    // Handle long-running reloads on tables using setEnabled for schemaCatalogCBox
    if (running.get()) {
      return;
    }

    int connectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());
    if (connectionId <= 0) {
      return;
    }

    running.set(true);
    adHocPanel.setClearFlag(true);
    adHocPanel.getDetailsControlPanelHandler().clearAll();
    adHocPanel.cleanVisualizeAndAnalyze();
    columnCase.removeAllElements();
    metricCase.removeAllElements();
    timestampCase.removeAllElements();

    viewCase.setBlockRunAction(true);
    tableCase.setBlockRunAction(true);
    viewCase.removeAllElements();
    tableCase.removeAllElements();
    viewCase.setBlockRunAction(false);
    tableCase.setBlockRunAction(false);

    tableNameAdHocTitle.setTitle("Table name: not selected");

    adHocPanel.setClearFlag(false);

    connectionName = GUIHelper.getNameByColumnName(connectionCase,
                                                   connectionCase.getJxTable().getSelectionModel(),
                                                   ConnectionColumnNames.NAME.getColName());

    periodicTask = Thread.ofVirtual().start(() -> {
      while (running.get()) {
        try {
          connectionCase.getJxTable().setEnabled(false);

          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connectionId);

          DatabaseMetaData metaData = connection.getMetaData();

          processSchema(connectionInfo, metaData);

          running.set(false);
          connectionName = "";
          connectionCase.getJxTable().setEnabled(true);
        } catch (Exception e) {
          DialogHelper.showErrorDialog(null,
                                       "Error on loading tables for connection: " + connectionName, "Connection error", e);

          running.set(false);
          connectionName = "";
          schemaCatalogCBox.setEnabled(true);
          connectionCase.getJxTable().setEnabled(true);
          periodicTask.interrupt();

          log.info(e);
          throw new RuntimeException(e);
        }
      }
    });
  }

  private void loadMetadata(int connectionId) {
    if (connectionId > 0) {
      ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connectionId);
      try {
        connectionCase.getJxTable().setEnabled(false);

        adHocDatabaseManager.createDataBase(connectionInfo);
        connection = connectionPoolManager.getDatasource(connectionInfo).getConnection();
        DatabaseMetaData metaData = connection.getMetaData();

        List<String> schemasCatalogs = getSchemasCatalogs(connectionInfo, metaData);
        schemasCatalogs.forEach(schemaCatalogCBox::addItem);

        processSchema(connectionInfo, metaData);

        running.set(false);
        connectionName = "";
        connectionCase.getJxTable().setEnabled(true);
      } catch (SQLException e) {
        DialogHelper.showErrorDialog(null, "Error on loading connection: " + connectionName, "Connection error", e);

        running.set(false);
        connectionName = "";
        schemaCatalogCBox.setEnabled(true);
        connectionCase.getJxTable().setEnabled(true);
        periodicTask.interrupt();

        log.info(e);
        throw new RuntimeException(e);
      }
    }
  }

  private void processSchema(ConnectionInfo connectionInfo, DatabaseMetaData metaData) {
    log.info("Start schema processing");

    String selectedSchemaCatalog = (String) schemaCatalogCBox.getSelectedItem();
    schemaCatalogCBox.setEnabled(false);

    if (POSTGRES.equals(connectionInfo.getDbType())) {
      processSchemaDatabaseMetaDataPostgres(metaData, selectedSchemaCatalog);
    } else if (CLICKHOUSE.equals(connectionInfo.getDbType())) {
      processCatalogDatabaseMetaDataClickHouse(metaData, selectedSchemaCatalog);
    } else if (ORACLE.equals(connectionInfo.getDbType())) {
      processSchemaFilterDatabaseMetaDataOracle(metaData, selectedSchemaCatalog);
    } else if (MSSQL.equals(connectionInfo.getDbType())) {
      processSchemaDatabaseMetaDataMSSQL(metaData, selectedSchemaCatalog);
    } else if (MYSQL.equals(connectionInfo.getDbType())) {
      processSchemaDatabaseMetaDataMySQL(metaData, selectedSchemaCatalog);
    } else if (DUCKDB.equals(connectionInfo.getDbType())) {
      processCatalogDatabaseMetaDataDuckDB(metaData, selectedSchemaCatalog);
    }

    schemaCatalogCBox.setEnabled(true);

    log.info("Stop schema processing");
  }

  private List<String> getSchemasCatalogs(ConnectionInfo connectionInfo,
                                          DatabaseMetaData metaData) throws SQLException {
    if (POSTGRES.equals(connectionInfo.getDbType())) {
      return getSchemas(metaData);
    } else if (ORACLE.equals(connectionInfo.getDbType())) {
      return getSchemas(metaData);
    } else if (MSSQL.equals(connectionInfo.getDbType())) {
      return getSchemas(metaData, excludedSchemasMSSQL);
    } else if (CLICKHOUSE.equals(connectionInfo.getDbType())) {
      return getCatalogs(metaData);
    } else if (MYSQL.equals(connectionInfo.getDbType())) {
      return getCatalogs(metaData, excludedSchemasMySQL);
    } else if (DUCKDB.equals(connectionInfo.getDbType())) {
      return getCatalogs(metaData);
    }
    return new ArrayList<>();
  }

  private void processEntities(DatabaseMetaData metaData,
                               String selectedSchemaCatalog,
                               Function<DatabaseMetaData, List<String>> entityProvider,
                               String entityType,
                               JXTableCase entityCase) {
    entityProvider.apply(metaData).stream()
        .filter(schema -> schema.equals(selectedSchemaCatalog))
        .forEach(schema -> {
          try {
            ResultSet resultSet = metaData.getTables(schema, selectedSchemaCatalog, null, new String[]{entityType});
            fillEntityTable(resultSet, entityCase);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void processEntitiesMSSQL(DatabaseMetaData metaData,
                                    String selectedSchemaCatalog,
                                    Function<DatabaseMetaData, List<String>> entityProvider,
                                    String entityType,
                                    JXTableCase entityCase) {
    entityProvider.apply(metaData).stream()
        .filter(schema -> schema.equals(selectedSchemaCatalog))
        .forEach(schema -> {
          try {
            ResultSet resultSet = metaData.getTables(null, schema, null, new String[]{entityType});
            fillEntityTable(resultSet, entityCase);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void processEntitiesFilteredOracle(DatabaseMetaData metaData,
                                             String selectedSchemaCatalog,
                                             Function<DatabaseMetaData, List<String>> entityProvider,
                                             String schemaName,
                                             String entityType,
                                             JXTableCase entityCase) {
    log.info("Start procedure to getTables");

    entityProvider.apply(metaData).stream()
        .filter(schema -> schema.equals(selectedSchemaCatalog))
        .forEach(schema -> {
          if (schemaName.equalsIgnoreCase(schema)) {

            if (schemaName.equalsIgnoreCase(schema)) {
              String sql = "SELECT o.object_name AS table_name " +
                  "FROM all_objects o " +
                  "WHERE o.owner = ? " +
                  "  AND o.object_type = ? " +
                  "ORDER BY table_name";

              try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, schema.toUpperCase());
                statement.setString(2, entityType.toUpperCase());

                try (ResultSet resultSet = statement.executeQuery()) {
                  fillEntityTable(resultSet, entityCase);
                }
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            }
          }
        });

    log.info("Stop procedure to getTables");
  }

  public void processSchemaDatabaseMetaDataPostgres(DatabaseMetaData metaData,
                                                    String selectedSchema) {
    processEntities(metaData, selectedSchema, this::getSchemas, "TABLE", tableCase);
    processEntities(metaData, selectedSchema, this::getSchemas, "VIEW", viewCase);
  }

  public void processCatalogDatabaseMetaDataClickHouse(DatabaseMetaData metaData,
                                                       String selectedCatalog) {
    processEntities(metaData, selectedCatalog, this::getCatalogs, "TABLE", tableCase);
    processEntities(metaData, selectedCatalog, this::getCatalogs, "VIEW", viewCase);
  }

  public void processSchemaFilterDatabaseMetaDataOracle(DatabaseMetaData metaData,
                                                        String selectedSchema) {
    processEntitiesFilteredOracle(metaData, selectedSchema, this::getSchemas, selectedSchema, "TABLE", tableCase);
    processEntitiesFilteredOracle(metaData, selectedSchema, this::getSchemas, selectedSchema, "VIEW", viewCase);
  }

  public void processSchemaDatabaseMetaDataMSSQL(DatabaseMetaData metaData,
                                                 String selectedSchema) {
    processEntitiesMSSQL(metaData, selectedSchema, md -> getSchemas(md, excludedSchemasMSSQL), "TABLE", tableCase);
    processEntitiesMSSQL(metaData, selectedSchema, md -> getSchemas(md, excludedSchemasMSSQL), "VIEW", viewCase);
  }

  public void processSchemaDatabaseMetaDataMySQL(DatabaseMetaData metaData,
                                                 String selectedSchema) {
    processEntities(metaData, selectedSchema, this::getCatalogs, "TABLE", tableCase);
    processEntities(metaData, selectedSchema, this::getCatalogs, "VIEW", viewCase);
  }

  public void processCatalogDatabaseMetaDataDuckDB(DatabaseMetaData metaData,
                                                   String selectedCatalog) {
    processEntities(metaData, selectedCatalog, this::getCatalogs, "TABLE", tableCase);
    processEntities(metaData, selectedCatalog, this::getCatalogs, "VIEW", viewCase);
  }

  public void fillEntityTable(ResultSet resultSet,
                              JXTableCase entityCase) {
    try {
      while (resultSet.next()) {
        String entityName = resultSet.getString("TABLE_NAME");
        entityCase.getDefaultTableModel().addRow(new Object[]{entityCase.getJxTable().getRowCount(), entityName});
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        resultSet.close();
      } catch (SQLException e) {
        log.error("Error closing ResultSet", e);
      }
    }
  }

  public List<String> getCatalogs(DatabaseMetaData metaData) {
    List<String> list = new ArrayList<>();
    try {
      ResultSet catalogs = metaData.getCatalogs();

      while (catalogs.next()) {
        String catalogName = catalogs.getString(1);
        list.add(catalogName);
      }
      catalogs.close();
    } catch (Exception e) {
      log.catching(e);
    }

    return list;
  }

  public List<String> getSchemas(DatabaseMetaData metaData,
                                 Set<String> excludedSchemas) {
    return retrieveSchemas(metaData, excludedSchemas);
  }

  public List<String> getCatalogs(DatabaseMetaData metaData,
                                 Set<String> excludedSchemas) {
    return retrieveCatalogs(metaData, excludedSchemas);
  }

  public List<String> getSchemas(DatabaseMetaData metaData) {
    return retrieveSchemas(metaData, excludedSchemasOracle);
  }

  private List<String> retrieveSchemas(DatabaseMetaData metaData,
                                       Set<String> excludedSchemas) {
    List<String> schemasList = new ArrayList<>();

    try (ResultSet schemas = metaData.getSchemas()) {
      while (schemas.next()) {
        String schema = schemas.getString(1);
        if (excludedSchemas == null || !excludedSchemas.contains(schema)) {
          schemasList.add(schema);
        }
      }
    } catch (Exception e) {
      log.catching(e);
    }

    return schemasList;
  }

  private List<String> retrieveCatalogs(DatabaseMetaData metaData,
                                       Set<String> excludedCatalogs) {
    List<String> schemasList = new ArrayList<>();

    try (ResultSet schemas = metaData.getCatalogs()) {
      while (schemas.next()) {
        String schema = schemas.getString(1);
        if (excludedCatalogs == null || !excludedCatalogs.contains(schema)) {
          schemasList.add(schema);
        }
      }
    } catch (Exception e) {
      log.catching(e);
    }

    return schemasList;
  }

  @Override
  public void fireShowAdHoc() {
    this.adHocView.showAdHoc();
  }

  public <T> void fillModel(Class<T> clazz) {
    if (ConfigClasses.Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Connection..");
      configurationManager.getConfigList(Connection.class).stream()
          .filter(e -> e.getType() == null || e.getType().equals(ConnectionType.JDBC))
          .forEach(e -> connectionCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));
    }
  }
}
