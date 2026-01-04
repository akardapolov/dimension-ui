package ru.dimension.ui.component.module.adhoc.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.db.DatabaseMetadata;
import ru.dimension.ui.component.module.db.MetadataFactory;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.AdHocStateManager;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.EntityRow;
import ru.dimension.ui.view.table.row.Rows.TimestampRow;

@Log4j2
public class AdHocModelPresenter implements HelperChart {

  private final AdHocModelModel model;
  private final AdHocModelView view;
  private final MessageBroker broker = MessageBroker.getInstance();
  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean removingConnection = new AtomicBoolean(false);

  private Thread periodicTask;
  private java.sql.Connection connection;
  private String connectionName = "";
  private DStore dStore;
  private TProfile tProfile;

  private int currentConnectionId;

  public AdHocModelPresenter(AdHocModelModel model, AdHocModelView view) {
    this.model = model;
    this.view = view;

    setupListeners();
    setupCheckboxListener();
  }

  public void loadConnections() {
    loadModel(Connection.class);
  }

  public void addConnection(int connectionId, String connectionName, ConnectionType type) {
    if (type == null || type.equals(ConnectionType.JDBC)) {
      ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
      DBType dbType = connectionInfo != null ? connectionInfo.getDbType() : null;

      SwingUtilities.invokeLater(() ->
                                     view.addConnectionRow(new ConnectionRow(connectionId, connectionName, ConnectionType.JDBC, dbType))
      );
      log.info("Added connection to AdHoc model: id={}, name={}, dbType={}",
               connectionId, connectionName, dbType);
    }
  }

  public void removeConnection(int connectionId) {
    SwingUtilities.invokeLater(() -> {
      removingConnection.set(true);
      try {
        boolean isCurrentConnection = (currentConnectionId == connectionId);

        if (isCurrentConnection) {
          removeAllChartsForConnection(connectionId);
          clearUIElements();
          releaseConnection(connectionId);

          tProfile = null;
          dStore = null;
          connection = null;
          currentConnectionId = 0;
          connectionName = "";
        } else {
          removeAllChartsForConnection(connectionId);
          releaseConnection(connectionId);
        }

        model.clearSelectionState(connectionId);
        view.removeConnectionRowById(connectionId);
        log.info("Removed connection from AdHoc model: id={}", connectionId);

        if (isCurrentConnection && view.getConnectionTable().model().getRowCount() > 0) {
          view.getConnectionTable().table().clearSelection();
        }
      } finally {
        removingConnection.set(false);
      }
    });
  }

  private void removeAllChartsForConnection(int connectionId) {
    Map<String, Set<Integer>> selectionState = model.getSelectionStateForConnection(connectionId);

    if (selectionState.isEmpty()) {
      log.info("No charts to remove for connection: {}", connectionId);
      return;
    }

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.REMOVE_ALL_CHARTS_FOR_CONNECTION)
                           .parameter("connectionId", connectionId)
                           .build());

    log.info("Sent remove all charts message for connection: {}", connectionId);
  }

  private void releaseConnection(int connectionId) {
    try {
      model.getAdHocDatabaseManager().removeDataBase(connectionId);
      model.getConnectionPoolManager().removeConnection(connectionId);
      log.info("Released connection pool resources for connectionId: {}", connectionId);
    } catch (Exception e) {
      log.error("Error releasing connection for connectionId: {}", connectionId, e);
    }
  }

  private void setupListeners() {
    view.getConnectionTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !view.isBlockConnectionAction()) {
        ConnectionRow row = view.getSelectedConnectionRow();
        if (row != null) {
          runActionConnection(row.getName());
        }
      }
    });

    view.getSchemaCatalogCBox().addActionListener(e -> reloadTables());

    view.getTableTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !view.isBlockTableAction()) {
        runActionForSelectedEntity(view.getTableTable(), "Table");
      }
    });

    view.getViewTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !view.isBlockViewAction()) {
        runActionForSelectedEntity(view.getViewTable(), "View");
      }
    });

    view.getTimestampTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !view.isBlockTimestampAction()) {
        TimestampRow row = view.getSelectedTimestampRow();
        if (row != null) {
          runActionTimestamp(row.getName());
        }
      }
    });

    view.getTableViewPane().addChangeListener(event -> triggerActionForSelectedRow());
  }

  private void setupCheckboxListener() {
    view.setCheckboxChangeListener(new AdHocModelView.EntityCheckboxChangeListener() {
      @Override
      public void onTableCheckboxChanged(EntityRow row, boolean selected, int modelRow) {
        handleTableSelection(selected, modelRow);
      }

      @Override
      public void onViewCheckboxChanged(EntityRow row, boolean selected, int modelRow) {
        handleViewSelection(selected, modelRow);
      }

      @Override
      public void onColumnCheckboxChanged(ColumnRow row, boolean selected, int modelRow) {
        handleColumnSelection(selected, modelRow);
      }
    });
  }

  private void runActionForSelectedEntity(TTTable<EntityRow, JXTable> entityTable, String entityType) {
    int viewRow = entityTable.table().getSelectedRow();
    if (viewRow < 0) return;

    int modelRow = entityTable.table().convertRowIndexToModel(viewRow);
    EntityRow row = entityTable.model().itemAt(modelRow);
    if (row == null) return;

    String name = row.getName();
    String schema = (String) view.getSchemaCatalogCBox().getSelectedItem();
    String fullName = (schema == null || schema.isBlank()) ? name : schema + "." + name;

    runAction(fullName);
  }

  private void handleTableSelection(boolean selected, int row) {
    handleEntitySelection(view.getTableTable(), row, selected);
  }

  private void handleViewSelection(boolean selected, int row) {
    handleEntitySelection(view.getViewTable(), row, selected);
  }

  private void handleEntitySelection(TTTable<EntityRow, JXTable> entityTable, int row, boolean selected) {
    EntityRow entityRow = entityTable.model().itemAt(row);
    if (entityRow == null) return;

    String name = entityRow.getName();
    String schema = (String) view.getSchemaCatalogCBox().getSelectedItem();
    String fullName = (schema == null || schema.isBlank()) ? name : schema + "." + name;

    if (selected) {
      view.setIgnoreTableCheckboxEvents(true);
      view.setIgnoreViewCheckboxEvents(true);
      try {
        entityRow.setPick(false);
      } finally {
        view.setIgnoreTableCheckboxEvents(false);
        view.setIgnoreViewCheckboxEvents(false);
      }
      return;
    }

    model.setTableOrViewSelected(currentConnectionId, fullName, false);
    unselectAllColumnsForTable(fullName);
  }

  private void unselectAllColumnsForTable(String fullTableName) {
    Set<Integer> columnIds = model.getSelectedColumns(currentConnectionId, fullTableName);
    for (int columnId : columnIds) {
      model.setColumnSelected(currentConnectionId, fullTableName, columnId, false);
    }

    if (tProfile != null && fullTableName.equals(tProfile.getTableName())) {
      view.clearAllColumnCheckboxes();
    }

    try {
      ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(currentConnectionId);
      if (connectionInfo != null) {
        String globalKey = KeyHelper.getGlobalKey(connectionInfo, fullTableName);
        broker.sendMessage(Message.builder()
                               .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                               .action(Action.REMOVE_ALL_CHARTS_FOR_TABLE_OR_VIEW)
                               .parameter("globalKey", globalKey)
                               .build());
      }
    } catch (Exception e) {
      log.error("Failed to remove charts for table {}", fullTableName, e);
    }
  }

  private void handleColumnSelection(boolean selected, int row) {
    if (tProfile == null) {
      DialogHelper.showMessageDialog(null, "Table metadata not loaded", "Error");
      view.setColumnPickValue(row, false);
      return;
    }

    ColumnRow columnRow = view.getColumnTable().model().itemAt(row);
    if (columnRow == null) return;

    int columnId = columnRow.getId();
    String tableName = tProfile != null ? tProfile.getTableName() : "";

    model.setColumnSelected(currentConnectionId, tableName, columnId, selected);

    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());
    TTTable<EntityRow, JXTable> activeTable = switch (activeTab) {
      case "Table" -> view.getTableTable();
      case "View" -> view.getViewTable();
      default -> null;
    };

    if (selected) {
      boolean wasSelected = model.isTableOrViewSelected(currentConnectionId, tableName);
      if (!wasSelected) {
        model.setTableOrViewSelected(currentConnectionId, tableName, true);
      }
      updateTableCheckboxState(activeTable, tableName, true);
      addChart(tableName, columnId);
    } else {
      Set<Integer> selectedColumns = model.getSelectedColumns(currentConnectionId, tableName);
      if (selectedColumns.isEmpty()) {
        model.setTableOrViewSelected(currentConnectionId, tableName, false);
        updateTableCheckboxState(activeTable, tableName, false);
      }
      removeChart(tableName, columnId);
    }
  }

  private void updateTableCheckboxState(TTTable<EntityRow, JXTable> targetTable, String fullTableName, boolean selected) {
    if (targetTable == null) return;

    String baseName = fullTableName.contains(".")
        ? fullTableName.substring(fullTableName.lastIndexOf('.') + 1)
        : fullTableName;

    view.updateEntityCheckbox(targetTable, baseName, selected);
  }

  private void triggerActionForSelectedRow() {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());
    TTTable<EntityRow, JXTable> activeTable = switch (activeTab) {
      case "Table" -> view.getTableTable();
      case "View" -> view.getViewTable();
      default -> null;
    };

    view.clearTimestampTable();
    view.clearColumnTable();

    tProfile = null;
    dStore = null;

    if (activeTable == null) return;

    int row = activeTable.table().getSelectedRow();
    if (row < 0) return;

    int modelRow = activeTable.table().convertRowIndexToModel(row);
    EntityRow entityRow = activeTable.model().itemAt(modelRow);
    if (entityRow == null) return;

    String name = entityRow.getName();
    if (name == null || name.isBlank()) return;

    String schema = (String) view.getSchemaCatalogCBox().getSelectedItem();
    String fullName = (schema == null || schema.isBlank()) ? name : schema + "." + name;

    SwingUtilities.invokeLater(() -> runAction(fullName));
  }

  private void runActionConnection(String name) {
    if (running.get()) {
      int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                "Loading metadata for connection " + connectionName
                                                    + ". Do you want to interrupt loading?");
      if (input == 0) {
        running.set(false);
        periodicTask.interrupt();
      }
      return;
    }

    running.set(true);
    clearUIElements();

    ConnectionRow connectionRow = view.getSelectedConnectionRow();
    if (connectionRow == null) return;

    currentConnectionId = connectionRow.getId();
    connectionName = connectionRow.getName();

    periodicTask = Thread.ofVirtual().start(() -> {
      loadMetadata(currentConnectionId);
      running.set(false);
    });
  }

  private void runAction(String schemaDotTableOrViewName) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    TTTable<EntityRow, JXTable> activeTable = switch (activeTab) {
      case "Table" -> view.getTableTable();
      case "View" -> view.getViewTable();
      default -> null;
    };

    if (activeTable == null) {
      clearColumnAndTimestamp();
      return;
    }

    int selectedRow = activeTable.table().getSelectedRow();
    if (selectedRow < 0) {
      clearColumnAndTimestamp();
      return;
    }

    if (schemaDotTableOrViewName == null || schemaDotTableOrViewName.isBlank()) {
      clearColumnAndTimestamp();
      return;
    }

    log.info("Run action for {}: {}", activeTab.toLowerCase(), schemaDotTableOrViewName);
    cleanAllPanels();
    loadTableView(schemaDotTableOrViewName);
  }

  private void runActionTimestamp(String timestampName) {
    log.info("Run action for timestamp: {}", timestampName);
    if (timestampName.isBlank()) return;
    setTimestampColumn(timestampName);
  }

  private void setTimestampColumn(String timestampName) {
    log.info("Timestamp column: {}", timestampName);
    try {
      if (tProfile != null) {
        dStore.setTimestampColumn(tProfile.getTableName(), timestampName);
        tProfile = dStore.getTProfile(tProfile.getTableName());
      }
    } catch (TableNameEmptyException e) {
      log.error("Error setting timestamp", e);
    }
  }

  private void addChart(String tableName, int cProfileId) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    log.info("{} name: {}", activeTab, tableName);
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();
    log.info("Column profile: {}", cProfile);

    ConnectionRow connectionRow = view.getSelectedConnectionRow();
    if (connectionRow == null) return;

    int connectionId = connectionRow.getId();
    ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
    TableInfo tableInfo = new TableInfo(tProfile);
    tableInfo.setDimensionColumnList(new ArrayList<>());

    String queryText = buildMetadataQuery(tableName, connectionInfo.getDbType());

    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setId(tableName.hashCode());
    queryInfo.setName(tableName);
    queryInfo.setText(queryText);

    ChartInfo chartInfo = new ChartInfo();
    chartInfo.setId(queryInfo.getId());

    AdHocKey adHocKey = KeyHelper.getAdHocKey(connectionInfo, tableName, cProfile);
    String globalKey = KeyHelper.getGlobalKey(connectionInfo, tableName);

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(adHocKey, globalKey);
    ChartRange chartRange = adHocStateManager.getCustomChartRange(adHocKey, globalKey);

    chartInfo.setRangeHistory(Objects.requireNonNullElse(rangeHistory, RangeHistory.DAY));

    boolean isEmptyRange = false;
    if (chartRange == null) {
      chartRange = getChartRange(dStore, tableInfo.getTableName(), chartInfo);
      rangeHistory = chartInfo.getRangeHistory();

      if (RangeHistory.CUSTOM.equals(chartInfo.getRangeHistory())) {
        if (chartRange.getBegin() == chartRange.getEnd()) {
          log.warn("Here the custom begin and end are identical. Fix it to add 1 second to the end of range");
          chartRange.setEnd(chartRange.getBegin() + 1000L);
        }
      }

      isEmptyRange = true;
    }

    if (RangeHistory.CUSTOM.equals(rangeHistory)) {
      chartInfo.setRangeHistory(RangeHistory.CUSTOM);
    }

    chartInfo.setCustomBegin(chartRange.getBegin());
    chartInfo.setCustomEnd(chartRange.getEnd());

    if (isEmptyRange) {
      adHocStateManager.putHistoryRange(adHocKey, rangeHistory);
      adHocStateManager.putHistoryCustomRange(adHocKey, chartRange);

      adHocStateManager.putGlobalHistoryRange(globalKey, rangeHistory);
      adHocStateManager.putGlobalHistoryCustomRange(globalKey, chartRange);

      adHocStateManager.putGlobalShowLegend(globalKey, true);
      adHocStateManager.putGlobalChartCardState(globalKey, ChartCardState.COLLAPSE_ALL);
    }

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CONFIG))
                           .action(Action.HISTORY_CUSTOM_UI_RANGE_CHANGE)
                           .parameter("globalKey", globalKey)
                           .build());

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.ADD_CHART)
                           .parameter("connectionInfo", connectionInfo)
                           .parameter("activeTab", activeTab)
                           .parameter("tableName", tableName)
                           .parameter("cProfile", cProfile)
                           .parameter("tableInfo", tableInfo)
                           .parameter("queryInfo", queryInfo)
                           .parameter("chartInfo", chartInfo)
                           .build());
  }

  private void removeChart(String tableName, int cProfileId) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    log.info("{} name: {}", activeTab, tableName);
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();
    log.info("Column profile: {}", cProfile);

    ConnectionRow connectionRow = view.getSelectedConnectionRow();
    if (connectionRow == null) return;

    int connectionId = connectionRow.getId();
    ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.REMOVE_CHART)
                           .parameter("connectionInfo", connectionInfo)
                           .parameter("activeTab", activeTab)
                           .parameter("tableName", tableName)
                           .parameter("cProfile", cProfile)
                           .build());
  }

  private void loadTableView(String tableName) {
    log.info("Table/view name: {}", tableName);
    view.getStatusLabel().setText("Table/view name: " + tableName + " is loading..");

    ConnectionRow connectionRow = view.getSelectedConnectionRow();
    if (connectionRow == null) return;

    int connectionId = connectionRow.getId();
    try {
      ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
      java.sql.Connection connection = model.getConnectionPoolManager().getConnection(connectionInfo);

      String queryText = buildMetadataQuery(tableName, connectionInfo.getDbType());

      dStore = model.getAdHocDatabaseManager().getDataBase(connectionInfo);
      tProfile = dStore.loadJdbcTableMetadata(connection, queryText, getSProfile(tableName, connectionInfo.getDbType()));
      log.info("Loaded metadata for table: {}", tableName);

      if (tProfile.getCProfiles() == null) {
        throw new RuntimeException("Error while loading metadata for table name: " + tableName);
      }

      TableInfo tableInfo = new TableInfo(tProfile);
      tableInfo.setDimensionColumnList(new ArrayList<>());
      createQueryInfo(tableName, queryText);

      updateColumnDisplays(tProfile.getCProfiles(), tableName);

      if (view.getTimestampTable().model().getRowCount() == 0) {
        handleNoTimestampColumns(tableName);
        return;
      } else {
        selectFirstTimestamp();
      }

      view.getStatusLabel().setText("Table/view name: " + tableName);
    } catch (Exception e) {
      handleTableViewError(e);
    }
  }

  private String buildMetadataQuery(String tableName, DBType dbType) {
    return switch (dbType) {
      case ORACLE -> "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
      case MSSQL -> "SELECT TOP 1 * FROM " + tableName;
      case FIREBIRD -> "SELECT FIRST 1 * FROM " + tableName;
      default -> "SELECT * FROM " + tableName + " LIMIT 1";
    };
  }

  private void createQueryInfo(String tableName, String queryText) {
    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setId(tableName.hashCode());
    queryInfo.setName(tableName);
    queryInfo.setText(queryText);

    ChartInfo chartInfo = new ChartInfo();
    chartInfo.setId(queryInfo.getId());
    model.getProfileManager().updateChart(chartInfo);
  }

  private void handleNoTimestampColumns(String tableName) {
    view.getStatusLabel().setText("For " + tableName + " timestamp column not found");
    view.clearColumnTable();
  }

  private void selectFirstTimestamp() {
    view.setBlockTimestampAction(true);
    view.getTimestampTable().table().setRowSelectionInterval(0, 0);
    TimestampRow row = view.getSelectedTimestampRow();
    if (row != null) {
      setTimestampColumn(row.getName());
    }
    view.setBlockTimestampAction(false);
  }

  private void handleTableViewError(Exception e) {
    view.getStatusLabel().setText(e.getMessage());
    view.clearColumnTable();
    view.clearTimestampTable();
    log.catching(e);
    throw new RuntimeException(e);
  }

  public SProfile getSProfile(String tableName, DBType dbType) {
    SProfile sProfile = new SProfile();
    sProfile.setTableName(tableName);
    sProfile.setTableType(TType.TIME_SERIES);
    sProfile.setIndexType(IType.GLOBAL);
    sProfile.setCompression(true);
    sProfile.setCsTypeMap(new HashMap<>());
    sProfile.setBackendType(switch (dbType) {
      case CLICKHOUSE -> BType.CLICKHOUSE;
      case POSTGRES -> BType.POSTGRES;
      case ORACLE -> BType.ORACLE;
      case MSSQL -> BType.MSSQL;
      case MYSQL -> BType.MYSQL;
      case DUCKDB -> BType.DUCKDB;
      case FIREBIRD -> BType.FIREBIRD;
      default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
    });
    return sProfile;
  }

  protected void cleanAllPanels() {
    log.info("Clean all panels implementation");
  }

  private void reloadTables() {
    if (running.get() || removingConnection.get()) return;

    if (connection == null) {
      log.debug("Connection is null, skipping reloadTables");
      return;
    }

    ConnectionRow connectionRow = view.getSelectedConnectionRow();
    if (connectionRow == null || connectionRow.getId() <= 0) return;

    int connectionId = connectionRow.getId();
    ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
    if (connectionInfo == null) {
      log.debug("ConnectionInfo is null for connectionId: {}, skipping reloadTables", connectionId);
      return;
    }

    running.set(true);
    clearUIElementsReloadTables();
    connectionName = connectionRow.getName();

    periodicTask = Thread.ofVirtual().start(() -> {
      try {
        DatabaseMetaData metaData = connection.getMetaData();
        processSchema(connectionInfo, metaData);
      } catch (Exception e) {
        DialogHelper.showErrorDialog(null,
                                     "Error on loading tables for connection: " + connectionName, "Connection error", e);
        log.error("Error reloading tables", e);
      } finally {
        running.set(false);
        connectionName = "";
        view.getSchemaCatalogCBox().setEnabled(true);
        view.getConnectionTable().table().setEnabled(true);
      }
    });
  }

  private void loadMetadata(int connectionId) {
    if (connectionId <= 0) return;

    try {
      ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
      if (connectionInfo == null) {
        log.warn("ConnectionInfo is null for connectionId: {}", connectionId);
        return;
      }

      model.getAdHocDatabaseManager().createDataBase(connectionInfo);
      connection = model.getConnectionPoolManager().getDatasource(connectionInfo).getConnection();
      DatabaseMetaData metaData = connection.getMetaData();

      getSchemasCatalogs(connectionInfo, metaData).forEach(view.getSchemaCatalogCBox()::addItem);
      processSchema(connectionInfo, metaData);
    } catch (SQLException e) {
      DialogHelper.showErrorDialog(null, "Error on loading connection: " + connectionName, "Connection error", e);
      log.error("Error loading metadata", e);
    }
  }

  private void processSchema(ConnectionInfo connectionInfo, DatabaseMetaData metaData) {
    if (connectionInfo == null) {
      log.warn("ConnectionInfo is null, skipping processSchema");
      return;
    }

    log.info("Start schema processing");
    String selectedSchemaCatalog = (String) view.getSchemaCatalogCBox().getSelectedItem();
    view.getSchemaCatalogCBox().setEnabled(false);

    DatabaseMetadata dbMetadata = MetadataFactory.create(connectionInfo.getDbType());

    try {
      try (ResultSet tablesResultSet =
          connectionInfo.getDbType() == DBType.ORACLE
              ? dbMetadata.getOracleTables(connection, selectedSchemaCatalog, "TABLE")
              : dbMetadata.getTables(metaData, selectedSchemaCatalog)) {

        fillEntityTable(tablesResultSet, view.getTableTable(), selectedSchemaCatalog);
      }

      try (ResultSet viewsResultSet =
          connectionInfo.getDbType() == DBType.ORACLE
              ? dbMetadata.getOracleTables(connection, selectedSchemaCatalog, "VIEW")
              : dbMetadata.getViews(metaData, selectedSchemaCatalog)) {

        fillEntityTable(viewsResultSet, view.getViewTable(), selectedSchemaCatalog);
      }

    } catch (SQLException e) {
      log.error("Error processing schema", e);
      DialogHelper.showErrorDialog(null,
                                   "Error loading metadata for: " + selectedSchemaCatalog,
                                   "Database Error", e);
    } finally {
      view.getSchemaCatalogCBox().setEnabled(true);
      log.info("Stop schema processing");
    }
  }

  private List<String> getSchemasCatalogs(ConnectionInfo connectionInfo, DatabaseMetaData metaData) throws SQLException {
    DatabaseMetadata dbMetadata = MetadataFactory.create(connectionInfo.getDbType());
    return switch (connectionInfo.getDbType()) {
      case POSTGRES, ORACLE, MSSQL -> dbMetadata.getSchemas(metaData);
      case CLICKHOUSE, DUCKDB, MYSQL -> dbMetadata.getCatalogs(metaData);
      case FIREBIRD -> new ArrayList<>();
      default -> new ArrayList<>();
    };
  }

  private void fillEntityTable(ResultSet resultSet, TTTable<EntityRow, JXTable> entityTable, String schema) throws SQLException {
    List<EntityRow> rows = new ArrayList<>();
    int id = 0;
    while (resultSet.next()) {
      String entityName = resultSet.getString("TABLE_NAME");
      String fullName = (schema == null || schema.isBlank()) ? entityName : schema + "." + entityName;
      boolean isSelected = model.isTableOrViewSelected(currentConnectionId, fullName);
      rows.add(new EntityRow(id++, entityName, isSelected));
    }
    entityTable.setItems(rows);
  }

  private void updateColumnDisplays(List<CProfile> cProfileList, String tableName) {
    List<ColumnRow> columnRows = new ArrayList<>();
    List<TimestampRow> timestampRows = new ArrayList<>();

    for (CProfile cProfile : cProfileList) {
      boolean isSelected = model.isColumnSelected(currentConnectionId, tableName, cProfile.getColId());

      switch (cProfile.getCsType().getDType()) {
        case TIMESTAMP, TIMESTAMPTZ, TIMETZ, TIME, DATETIME, SMALLDATETIME, DATE, DATE32, DATETIME2 ->
            timestampRows.add(new TimestampRow(cProfile.getColName()));
        default -> columnRows.add(new ColumnRow(cProfile, isSelected));
      }
    }

    view.setColumnRows(columnRows);
    view.setTimestampRows(timestampRows);
  }

  public <T> void loadModel(Class<T> clazz) {
    if (ConfigClasses.Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Loading connection..");
      model.getConfigurationManager().getConfigList(Connection.class).stream()
          .filter(e -> e.getType() == null || e.getType().equals(ConnectionType.JDBC))
          .forEach(e -> {
            ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(e.getId());
            DBType dbType = connectionInfo != null ? connectionInfo.getDbType() : null;
            view.addConnectionRow(new ConnectionRow(e.getId(), e.getName(), ConnectionType.JDBC, dbType));
          });
    }
  }

  private void clearUIElements() {
    resetUI(true);
  }

  private void clearUIElementsReloadTables() {
    resetUI(false);
  }

  private void resetUI(boolean clearSchemaCatalog) {
    view.getStatusLabel().setText("");
    view.clearColumnTable();
    view.clearTimestampTable();

    view.setBlockViewAction(true);
    view.setBlockTableAction(true);
    try {
      view.clearViewTable();
      view.clearTableTable();
    } finally {
      view.setBlockViewAction(false);
      view.setBlockTableAction(false);
    }

    if (clearSchemaCatalog) {
      view.getSchemaCatalogCBox().removeAllItems();
    }
  }

  public void clearSelectionForTableOrView(Message message) {
    String globalKey = message.parameters().get("globalKey");
    if (globalKey == null || globalKey.isBlank()) return;

    String[] parts = globalKey.split("_", 2);
    if (parts.length != 2) return;

    int connectionId;
    try {
      connectionId = Integer.parseInt(parts[0]);
    } catch (NumberFormatException e) {
      return;
    }
    String tableName = parts[1];

    model.setTableOrViewSelected(connectionId, tableName, false);

    for (Integer colId : model.getSelectedColumns(connectionId, tableName)) {
      model.setColumnSelected(connectionId, tableName, colId, false);
    }

    SwingUtilities.invokeLater(() -> {
      updateTableCheckboxState(view.getTableTable(), tableName, false);
      updateTableCheckboxState(view.getViewTable(), tableName, false);

      if (tProfile != null && tableName.equals(tProfile.getTableName())) {
        view.clearAllColumnCheckboxes();
      }

      view.getTimestampTable().table().clearSelection();
    });
  }

  private void clearColumnAndTimestamp() {
    view.clearColumnTable();
    view.clearTimestampTable();
  }
}