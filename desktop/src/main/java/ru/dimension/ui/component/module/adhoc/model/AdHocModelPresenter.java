package ru.dimension.ui.component.module.adhoc.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
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
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.component.module.db.DatabaseMetadata;
import ru.dimension.ui.component.module.db.MetadataFactory;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.column.QueryMetadataColumnNames;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.AdHocStateManager;

@Log4j2
public class AdHocModelPresenter implements HelperChart {

  private final AdHocModelModel model;
  private final AdHocModelView view;
  private final MessageBroker broker = MessageBroker.getInstance();
  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  private final JXTableCase connectionCase;
  private final JComboBox<String> schemaCatalogCBox;
  private final JXTableCase tableCase;
  private final JXTableCase viewCase;
  private final JXTableCase timestampCase;
  private final JXTableCase columnCase;
  private final AtomicBoolean running = new AtomicBoolean(false);

  private Thread periodicTask;
  private java.sql.Connection connection;
  private String connectionName = "";
  private DStore dStore;
  private TProfile tProfile;

  private int currentConnectionId;

  public AdHocModelPresenter(AdHocModelModel model,
                             AdHocModelView view) {
    this.model = model;
    this.view = view;
    this.connectionCase = view.getConnectionCase();
    this.schemaCatalogCBox = view.getSchemaCatalogCBox();
    this.tableCase = view.getTableCase();
    this.viewCase = view.getViewCase();
    this.timestampCase = view.getTimestampCase();
    this.columnCase = view.getColumnCase();

    setupListeners();
    setupCheckboxEditors();
  }

  public void loadConnections() {
    loadModel(Connection.class);
  }

  private void setupListeners() {
    Consumer<String> runActionConnection = this::runActionConnection;
    new TableSelectionHandler(connectionCase, ConnectionColumnNames.NAME.getColName(), runActionConnection);

    schemaCatalogCBox.addActionListener(e -> reloadTables());

    tableCase.getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !tableCase.isBlockRunAction()) {
        runActionForSelectedEntity(tableCase, "Table");
      }
    });

    viewCase.getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !viewCase.isBlockRunAction()) {
        runActionForSelectedEntity(viewCase, "View");
      }
    });

    Consumer<String> runActionTimestamp = this::runActionTimestamp;
    new TableSelectionHandler(timestampCase, ConnectionColumnNames.NAME.getColName(), runActionTimestamp);

    view.getTableViewPane().addChangeListener(event -> triggerActionForSelectedRow());
  }

  private void setupCheckboxEditors() {
    setupCheckboxEditor(view.getTableCase(), this::handleTableSelection);
    setupCheckboxEditor(view.getViewCase(), this::handleViewSelection);
    setupCheckboxEditor(view.getColumnCase(), this::handleColumnSelection);
  }

  private void setupCheckboxEditor(JXTableCase tableCase,
                                   SelectionHandler handler) {
    JXTable table = tableCase.getJxTable();
    int checkboxColumn = table.getColumnCount() - 1;
    TableColumn column = table.getColumnModel().getColumn(checkboxColumn);
    DefaultCellEditor editor = new DefaultCellEditor(new JCheckBox());
    column.setCellEditor(editor);

    editor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean cValue = (Boolean) editor.getCellEditorValue();

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = table.convertRowIndexToModel(viewRow);
        handler.handleSelection(cValue, modelRow);
      }

      @Override
      public void editingCanceled(ChangeEvent e) {
      }
    });
  }

  private void runActionForSelectedEntity(JXTableCase entityCase, String entityType) {
    int viewRow = entityCase.getJxTable().getSelectedRow();
    if (viewRow < 0) return;

    int modelRow = entityCase.getJxTable().convertRowIndexToModel(viewRow);
    String name = (String) entityCase.getDefaultTableModel().getValueAt(modelRow, ColumnNames.NAME.ordinal());
    String schema = (String) schemaCatalogCBox.getSelectedItem();
    String fullName = (schema == null || schema.isBlank()) ? name : schema + "." + name;

    runAction(fullName);
  }

  private void handleTableSelection(boolean selected,
                                    int row) {
    handleEntitySelection(view.getTableCase(), row, selected);
  }

  private void handleViewSelection(boolean selected,
                                   int row) {
    handleEntitySelection(view.getViewCase(), row, selected);
  }

  private void handleEntitySelection(JXTableCase entityCase,
                                     int row,
                                     boolean selected) {
    String name = (String) entityCase.getDefaultTableModel().getValueAt(row, ColumnNames.NAME.ordinal());
    String schema = (String) view.getSchemaCatalogCBox().getSelectedItem();
    String fullName = (schema == null || schema.isBlank()) ? name : schema + "." + name;

    if (selected) {
      entityCase.getDefaultTableModel().setValueAt(false, row, ColumnNames.PICK.ordinal());
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
      DefaultTableModel tableModel = columnCase.getDefaultTableModel();

      List<Integer> cProfileIdsToRemove = new ArrayList<>();
      for (int i = 0; i < tableModel.getRowCount(); i++) {
        Boolean isSelected = (Boolean) tableModel.getValueAt(i, ColumnNames.PICK.ordinal());
        if (isSelected != null && isSelected) {
          int cProfileId = (int) tableModel.getValueAt(i, ColumnNames.ID.ordinal());
          cProfileIdsToRemove.add(cProfileId);
        }
        tableModel.setValueAt(false, i, ColumnNames.PICK.ordinal());
      }

      String tableName = tProfile != null ? tProfile.getTableName() : "";
      for (int cProfileId : cProfileIdsToRemove) {
        removeChart(tableName, cProfileId);
      }
    }
  }

  private void handleColumnSelection(boolean selected, int row) {
    /*JXTable columnTable = view.getColumnCase().getJxTable();
    int modelRow = columnTable.convertRowIndexToModel(row); // Convert view row to model row*/

    if (tProfile == null) {
      DialogHelper.showMessageDialog(null, "Table metadata not loaded", "Error");
      view.getColumnCase().getDefaultTableModel().setValueAt(false, row, ColumnNames.PICK.ordinal());
      return;
    }

    int columnId = (int) view.getColumnCase()
        .getDefaultTableModel()
        .getValueAt(row, ColumnNames.ID.ordinal());
    String tableName = tProfile != null ? tProfile.getTableName() : "";

    model.setColumnSelected(currentConnectionId, tableName, columnId, selected);

    String activeTab = view.getTableViewPane()
        .getTitleAt(view.getTableViewPane().getSelectedIndex());
    JXTableCase activeCase = switch (activeTab) {
      case "Table" -> tableCase;
      case "View"  -> viewCase;
      default      -> null;
    };

    if (selected) {
      boolean wasSelected = model.isTableOrViewSelected(currentConnectionId, tableName);
      if (!wasSelected) {
        model.setTableOrViewSelected(currentConnectionId, tableName, true);
      }
      updateTableCheckboxState(activeCase, tableName, true);

      addChart(tableName, columnId);
    } else {
      Set<Integer> selectedColumns = model.getSelectedColumns(currentConnectionId, tableName);
      if (selectedColumns.isEmpty()) {
        model.setTableOrViewSelected(currentConnectionId, tableName, false);
        updateTableCheckboxState(activeCase, tableName, false);
      }

      removeChart(tableName, columnId);
    }
  }

  private void updateTableCheckboxState(JXTableCase targetCase,
                                        String fullTableName,
                                        boolean selected) {

    if (targetCase == null) return;

    String baseName = fullTableName.contains(".")
        ? fullTableName.substring(fullTableName.lastIndexOf('.') + 1)
        : fullTableName;

    for (int i = 0; i < targetCase.getDefaultTableModel().getRowCount(); i++) {
      String entityName = (String) targetCase.getDefaultTableModel()
          .getValueAt(i, ColumnNames.NAME.ordinal());
      if (baseName.equals(entityName)) {
        targetCase.getDefaultTableModel().setValueAt(selected, i, ColumnNames.PICK.ordinal());
        return;
      }
    }
  }

  private void triggerActionForSelectedRow() {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());
    JXTableCase activeCase = switch (activeTab) {
      case "Table" -> tableCase;
      case "View" -> viewCase;
      default -> null;
    };

    timestampCase.clearTable();
    columnCase.clearTable();

    tProfile = null;
    dStore = null;

    if (activeCase == null) {
      return;
    }

    int row = activeCase.getJxTable().getSelectedRow();
    if (row < 0) {
      return;
    }

    String name = GUIHelper.getNameByColumnName(activeCase,
                                                activeCase.getJxTable().getSelectionModel(),
                                                ConnectionColumnNames.NAME.getColName());
    if (name == null || name.isBlank()) {
      return;
    }

    String schema = (String) schemaCatalogCBox.getSelectedItem();
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

    currentConnectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());

    connectionName = GUIHelper.getNameByColumnName(connectionCase,
                                                   connectionCase.getJxTable()
                                                       .getSelectionModel(), ConnectionColumnNames.NAME.getColName());

    periodicTask = Thread.ofVirtual().start(() -> {
      loadMetadata(GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName()));
      running.set(false);
    });
  }

  private void runAction(String schemaDotTableOrViewName) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    JXTableCase activeCase = switch (activeTab) {
      case "Table" -> tableCase;
      case "View" -> viewCase;
      default -> null;
    };

    if (activeCase == null) {
      clearColumnAndTimestamp();
      return;
    }

    int selectedRow = activeCase.getJxTable().getSelectedRow();
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
    if (timestampName.isBlank()) {
      return;
    }

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

  private boolean isColumnEmptyOrNotSelected() {
    if (columnCase.getJxTable().getRowCount() == 0) {
      DialogHelper.showMessageDialog(null, "Columns list is empty. Please try another table or view", "Warning");
      return true;
    }
    String columnName = GUIHelper.getNameByColumnName(columnCase,
                                                      columnCase.getJxTable()
                                                          .getSelectionModel(), QueryMetadataColumnNames.NAME.getColName());
    if (columnName.isBlank()) {
      DialogHelper.showMessageDialog(null, "Column not selected, please select it", "Warning");
      return true;
    }
    return false;
  }

  private boolean isTimeStampColumnEmptyOrNotSelected() {
    if (timestampCase.getJxTable().getRowCount() == 0) {
      DialogHelper.showMessageDialog(null, "Timestamp columns list is empty. Please try another table or view", "Warning");
      return true;
    }
    String tsColumnName = GUIHelper.getNameByColumnName(timestampCase,
                                                        timestampCase.getJxTable()
                                                            .getSelectionModel(), QueryMetadataColumnNames.NAME.getColName());
    if (tsColumnName.isBlank()) {
      DialogHelper.showMessageDialog(null, "Timestamp column not selected, please pick it", "Warning");
      return true;
    }
    return false;
  }

  private void addChart(String tableName,
                        int cProfileId) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    log.info("{} name: {}", activeTab, tableName);
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();
    log.info("Column profile: {}", cProfile);

    int connectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());
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

    /*TODO AdHocStateManager usage*/
    AdHocKey adHocKey = KeyHelper.getAdHocKey(connectionInfo, tableName, cProfile);
    String globalKey = KeyHelper.getGlobalKey(connectionInfo, tableName);

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(adHocKey, globalKey);
    ChartRange chartRange = adHocStateManager.getCustomChartRange(adHocKey, globalKey);

    chartInfo.setRangeHistory(Objects.requireNonNullElse(rangeHistory, RangeHistory.DAY));

    boolean isEmptyRange = false;
    if (chartRange == null) {
      chartRange = getChartRange(dStore, tableInfo.getTableName(), chartInfo);
      rangeHistory = chartInfo.getRangeHistory();

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

  private void removeChart(String tableName,
                           int cProfileId) {
    String activeTab = view.getTableViewPane().getTitleAt(view.getTableViewPane().getSelectedIndex());

    log.info("{} name: {}", activeTab, tableName);
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();
    log.info("Column profile: {}", cProfile);

    int connectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());
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

    int connectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());
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

      if (timestampCase.getJxTable().getRowCount() == 0) {
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

  private String buildMetadataQuery(String tableName,
                                    DBType dbType) {
    return switch (dbType) {
      case ORACLE -> "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
      case MSSQL -> "SELECT TOP 1 * FROM " + tableName;
      default -> "SELECT * FROM " + tableName + " LIMIT 1";
    };
  }

  private void createQueryInfo(String tableName,
                               String queryText) {
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
    columnCase.clearTable();
  }

  private void selectFirstTimestamp() {
    timestampCase.setBlockRunAction(true);
    timestampCase.getJxTable().setRowSelectionInterval(0, 0);
    String tsColumnName = GUIHelper.getNameByColumnName(timestampCase.getJxTable(),
                                                        timestampCase.getDefaultTableModel(),
                                                        timestampCase.getJxTable().getSelectionModel(),
                                                        QueryMetadataColumnNames.NAME.getColName());
    setTimestampColumn(tsColumnName);
    timestampCase.setBlockRunAction(false);
  }

  private void handleTableViewError(Exception e) {
    view.getStatusLabel().setText(e.getMessage());
    columnCase.clearTable();
    timestampCase.clearTable();
    log.catching(e);
    throw new RuntimeException(e);
  }

  public SProfile getSProfile(String tableName,
                              DBType dbType) {
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
      default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
    });
    return sProfile;
  }

  protected void cleanAllPanels() {
    log.info("Clean all panels implementation");
  }

  private void reloadTables() {
    if (running.get()) {
      return;
    }
    int connectionId = GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());
    if (connectionId <= 0) {
      return;
    }

    running.set(true);
    clearUIElementsReloadTables();
    connectionName = GUIHelper.getNameByColumnName(connectionCase,
                                                   connectionCase.getJxTable()
                                                       .getSelectionModel(), ConnectionColumnNames.NAME.getColName());

    periodicTask = Thread.ofVirtual().start(() -> {
      try {
        ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
        DatabaseMetaData metaData = connection.getMetaData();
        processSchema(connectionInfo, metaData);
      } catch (Exception e) {
        DialogHelper.showErrorDialog(null,
                                     "Error on loading tables for connection: "
                                         + connectionName, "Connection error", e);
        log.error("Error reloading tables", e);
      } finally {
        running.set(false);
        connectionName = "";
        schemaCatalogCBox.setEnabled(true);
        connectionCase.getJxTable().setEnabled(true);
      }
    });
  }

  private void loadMetadata(int connectionId) {
    if (connectionId <= 0) {
      return;
    }

    try {
      ConnectionInfo connectionInfo = model.getProfileManager().getConnectionInfoById(connectionId);
      model.getAdHocDatabaseManager().createDataBase(connectionInfo);
      connection = model.getConnectionPoolManager().getDatasource(connectionInfo).getConnection();
      DatabaseMetaData metaData = connection.getMetaData();

      getSchemasCatalogs(connectionInfo, metaData).forEach(schemaCatalogCBox::addItem);
      processSchema(connectionInfo, metaData);
    } catch (SQLException e) {
      DialogHelper.showErrorDialog(null, "Error on loading connection: " + connectionName, "Connection error", e);
      log.error("Error loading metadata", e);
    }
  }

  private void processSchema(ConnectionInfo connectionInfo,
                             DatabaseMetaData metaData) {
    log.info("Start schema processing");
    String selectedSchemaCatalog = (String) schemaCatalogCBox.getSelectedItem();
    schemaCatalogCBox.setEnabled(false);

    DatabaseMetadata dbMetadata = MetadataFactory.create(connectionInfo.getDbType());

    try {
      try (ResultSet tablesResultSet =
          connectionInfo.getDbType() == DBType.ORACLE
              ? dbMetadata.getOracleTables(connection, selectedSchemaCatalog, "TABLE")
              : dbMetadata.getTables(metaData, selectedSchemaCatalog)) {

        fillEntityTable(tablesResultSet, tableCase, selectedSchemaCatalog);
      }

      try (ResultSet viewsResultSet =
          connectionInfo.getDbType() == DBType.ORACLE
              ? dbMetadata.getOracleTables(connection, selectedSchemaCatalog, "VIEW")
              : dbMetadata.getViews(metaData, selectedSchemaCatalog)) {

        fillEntityTable(viewsResultSet, viewCase, selectedSchemaCatalog);
      }

    } catch (SQLException e) {
      log.error("Error processing schema", e);
      DialogHelper.showErrorDialog(null,
                                   "Error loading metadata for: " + selectedSchemaCatalog,
                                   "Database Error", e);
    } finally {
      schemaCatalogCBox.setEnabled(true);
      log.info("Stop schema processing");
    }
  }

  private List<String> getSchemasCatalogs(ConnectionInfo connectionInfo,
                                          DatabaseMetaData metaData) throws SQLException {
    DatabaseMetadata dbMetadata = MetadataFactory.create(connectionInfo.getDbType());
    return switch (connectionInfo.getDbType()) {
      case POSTGRES, ORACLE, MSSQL -> dbMetadata.getSchemas(metaData);
      case CLICKHOUSE, DUCKDB, MYSQL -> dbMetadata.getCatalogs(metaData);
      default -> new ArrayList<>();
    };
  }

  private void fillEntityTable(ResultSet resultSet,
                               JXTableCase entityCase,
                               String schema) throws SQLException {
    while (resultSet.next()) {
      String entityName = resultSet.getString("TABLE_NAME");
      String fullName = (schema == null || schema.isBlank()) ? entityName : schema + "." + entityName;
      boolean isSelected = model.isTableOrViewSelected(currentConnectionId, fullName);

      entityCase.addRow(new Object[]{entityCase.getJxTable().getRowCount(), entityName, isSelected});
    }
  }

  private void updateColumnDisplays(List<CProfile> cProfileList,
                                    String tableName) {
    view.getColumnCase().clearTable();
    view.getTimestampCase().clearTable();

    for (CProfile cProfile : cProfileList) {
      boolean isSelected = model.isColumnSelected(currentConnectionId, tableName, cProfile.getColId());

      switch (cProfile.getCsType().getDType()) {
        case TIMESTAMP, TIMESTAMPTZ, TIMETZ, TIME, DATETIME, SMALLDATETIME, DATE, DATE32, DATETIME2 ->
            view.getTimestampCase().addRow(new Object[]{cProfile.getColName()});
        default -> view.getColumnCase().addRow(new Object[]{
            cProfile.getColId(),
            cProfile.getColName(),
            isSelected
        });
      }
    }
  }

  public <T> void loadModel(Class<T> clazz) {
    if (ConfigClasses.Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Loading connection..");
      model.getConfigurationManager().getConfigList(Connection.class).stream()
          .filter(e -> e.getType() == null || e.getType().equals(ConnectionType.JDBC))
          .forEach(e -> connectionCase.addRow(new Object[]{e.getId(), e.getName()}));
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
    columnCase.clearTable();
    timestampCase.clearTable();

    viewCase.setBlockRunAction(true);
    tableCase.setBlockRunAction(true);
    try {
      viewCase.clearTable();
      tableCase.clearTable();
    } finally {
      viewCase.setBlockRunAction(false);
      tableCase.setBlockRunAction(false);
    }

    if (clearSchemaCatalog) {
      schemaCatalogCBox.removeAllItems();
    }
  }

  private String getSelectedTableName(String tabSelected) {
    String schemaUI = (String) schemaCatalogCBox.getSelectedItem();
    String schema = (schemaUI == null || schemaUI.isBlank()) ? "" : schemaUI + ".";

    return switch (tabSelected) {
      case "Table" -> schema + GUIHelper.getNameByColumnName(
          tableCase.getJxTable(), tableCase.getDefaultTableModel(),
          tableCase.getJxTable().getSelectionModel(), ColumnNames.NAME.getColName());
      case "View" -> schema + GUIHelper.getNameByColumnName(
          viewCase.getJxTable(), viewCase.getDefaultTableModel(),
          viewCase.getJxTable().getSelectionModel(), ColumnNames.NAME.getColName());
      default -> "";
    };
  }

  private void clearColumnAndTimestamp() {
    columnCase.clearTable();
    timestampCase.clearTable();
  }

  private interface SelectionHandler {

    void handleSelection(boolean selected,
                         int row);
  }
}