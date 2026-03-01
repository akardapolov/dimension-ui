package ru.dimension.ui.view.handler.query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
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
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.UpdateMetadataColumnsEvent;
import ru.dimension.ui.collector.HttpLoader;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.view.dialog.TimestampColumnChooser;
import ru.dimension.ui.view.dialog.TimestampColumnChooser.TimestampResult;
import ru.dimension.ui.view.handler.CommonViewHandler;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;

@Log4j2
@Singleton
public final class QueryMetadataHandler implements ActionListener, CommonViewHandler, HttpLoader {

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final ProfileManager profileManager;
  private final ConnectionPoolManager connectionPoolManager;
  private final JXTableCase configMetadataCase;
  private final EventBus eventBus;

  private final ExporterParser exporterParser;
  private final HttpResponseFetcher httpResponseFetcher;
  private final DStore dStore;

  private final JCheckBox checkboxConfig;
  private final MetadataQueryPanel metadataQueryPanel;
  private final MetricQueryPanel metricQueryPanel;
  private final JTabbedPane mainQuery;
  private final ConfigTab configTab;

  @Inject
  public QueryMetadataHandler(@Named("profileManager") ProfileManager profileManager,
                              @Named("profileConfigCase") JXTableCase profileCase,
                              @Named("taskConfigCase") JXTableCase taskCase,
                              @Named("connectionConfigCase") JXTableCase connectionCase,
                              @Named("queryConfigCase") JXTableCase queryCase,
                              @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager,
                              @Named("configMetadataCase") JXTableCase configMetadataCase,
                              @Named("checkboxConfig") JCheckBox checkboxConfig,
                              @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                              @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel,
                              @Named("mainQueryTab") JTabbedPane mainQuery,
                              @Named("configTab") ConfigTab configTab,
                              @Named("exporterParser") ExporterParser exporterParser,
                              @Named("httpResponseFetcher") HttpResponseFetcher httpResponseFetcher,
                              @Named("localDB") DStore dStore,
                              @Named("eventBus") EventBus eventBus) {
    this.profileManager = profileManager;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.connectionPoolManager = connectionPoolManager;
    this.configMetadataCase = configMetadataCase;
    this.eventBus = eventBus;

    metadataQueryPanel.getTableName().setEditable(false);

    this.metadataQueryPanel = metadataQueryPanel;
    this.metricQueryPanel = metricQueryPanel;
    this.mainQuery = mainQuery;
    this.configTab = configTab;

    this.metadataQueryPanel.getLoadMetadata().addActionListener(this);
    this.metadataQueryPanel.getEditMetadata().addActionListener(this);
    this.metadataQueryPanel.getSaveMetadata().addActionListener(this);
    this.metadataQueryPanel.getCancelMetadata().addActionListener(this);

    this.exporterParser = exporterParser;
    this.httpResponseFetcher = httpResponseFetcher;
    this.dStore = dStore;

    this.checkboxConfig = checkboxConfig;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int queryID = getSelectedQueryId();
    if (queryID <= 0) {
      throw new NotSelectedRowException("Not selected query. Please select and try again!");
    }

    QueryInfo query = profileManager.getQueryInfoById(queryID);
    if (query == null) {
      throw new NotFoundException("Not found query: " + queryID);
    }

    TableInfo table = profileManager.getTableInfoByTableName(query.getName());
    if (table == null) {
      throw new NotFoundException("Not found table: " + query.getName());
    }

    List<String> profileNameRunning = getProfileNameRunning(queryID);

    if (e.getSource() == metadataQueryPanel.getLoadMetadata()) {
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The query is not selected. Please select and try again!");
      }

      if (!profileNameRunning.isEmpty()) {
        JOptionPane.showMessageDialog(null,
                                      "You can't load metadata for query " + query.getName()
                                          + " used by running profiles: "
                                          + String.join(",", profileNameRunning),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (table.getCProfiles() != null) {
        int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                  "Do you want to rewrite existing metadata for query "
                                                      + query.getName() + " ?");
        if (input != 0) {
          return;
        }
      } else {
        int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                  "Do you want to get metadata for query " + query.getName()
                                                      + " ?");
        if (input != 0) {
          return;
        }
      }

      CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
        try {
          loadMetadata();
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(null, ex.getMessage(),
                                        "General Error", JOptionPane.ERROR_MESSAGE);
          throw new RuntimeException(ex);
        }
        return null;
      }).exceptionally(exception -> {
        metadataQueryPanel.getEditMetadata().setEnabled(false);
        Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        throw new RuntimeException(cause);
      });

      future.thenRun(() -> JOptionPane.showMessageDialog(null, "Metadata download completed",
                                                         "Information", JOptionPane.INFORMATION_MESSAGE));
      return;
    }

    if (e.getSource() == metadataQueryPanel.getEditMetadata()) {
      setPanelView(false);
      return;
    }

    if (e.getSource() == metadataQueryPanel.getSaveMetadata()) {
      setPanelView(true);

      QueryInfo queryInfo = getQueryInfo(query.getId());
      TableInfo tableInfo = getTableInfo(queryInfo);

      if (tableInfo.getDimensionColumnList() == null) {
        tableInfo.setDimensionColumnList(new ArrayList<>());
      }

      tableInfo.setTableType(TType.valueOf(Objects.requireNonNull(metadataQueryPanel.getTableType().getSelectedItem()).toString()));
      tableInfo.setIndexType(IType.valueOf(Objects.requireNonNull(metadataQueryPanel.getTableIndex().getSelectedItem()).toString()));
      tableInfo.setCompression(metadataQueryPanel.getCompression().isSelected());
      tableInfo.setBackendType(BType.BERKLEYDB);

      ConnectionInfo connectionInfo = getConnectionInfo();
      try {
        connectionPoolManager.createDataSource(connectionInfo);
        Connection connection = connectionPoolManager.getConnection(connectionInfo);

        TProfile tProfile = dStore.loadJdbcTableMetadata(connection, queryInfo.getText(), tableInfo.getSProfile());
        tableInfo.setCProfiles(tProfile.getCProfiles());
      } catch (SQLException | TableNameEmptyException ex) {
        throw new RuntimeException(ex);
      }

      updateTableInfo(tableInfo);

      profileManager.updateQuery(queryInfo);
      profileManager.updateTable(tableInfo);

      fillTableUI(tableInfo);

      publishMetadataUpdate(queryInfo.getId(), queryInfo.getName(), tableInfo.getCProfiles());
      return;
    }

    if (e.getSource() == metadataQueryPanel.getCancelMetadata()) {
      if (queryCase.getJxTable().getSelectedRowCount() <= 0) {
        return;
      }

      int queryId = getSelectedQueryId();

      setPanelView(true);

      QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
      if (queryInfo == null) {
        throw new NotFoundException("Not found query: " + queryId);
      }

      TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
      if (tableInfo == null) {
        throw new NotFoundException("Not found table: " + queryInfo.getName());
      }

      List<List<?>> connectionData = new LinkedList<>();
      profileManager.getTaskInfoList().stream()
          .filter(f -> f.getQueryInfoList().stream().anyMatch(qId -> qId == queryId))
          .findAny()
          .ifPresentOrElse(t -> {
            ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(t.getConnectionId());
            if (connectionInfo == null) {
              throw new NotFoundException("Not found task: " + t.getConnectionId());
            }
            connectionData.add(new ArrayList<>(Arrays.asList(
                connectionInfo.getName(),
                connectionInfo.getUserName(),
                connectionInfo.getUrl(),
                connectionInfo.getJar(),
                connectionInfo.getDriver(),
                connectionInfo.getType()
            )));
          }, () -> log.info("Not found query by query id: {}", queryId));

      metadataQueryPanel.getQueryConnectionMetadataComboBox().setTableData(connectionData);

      metadataQueryPanel.getTableType().setSelectedItem(tableInfo.getTableType() != null ? tableInfo.getTableType() : TType.TIME_SERIES);
      metadataQueryPanel.getTableIndex().setSelectedItem(tableInfo.getIndexType() != null ? tableInfo.getIndexType() : IType.LOCAL);

      metadataQueryPanel.getCompression().setEnabled(Boolean.FALSE);
      metadataQueryPanel.getCompression().setSelected(Boolean.TRUE.equals(tableInfo.getCompression()));

      List<List<?>> timestampListAll = new LinkedList<>();
      List<List<?>> timestampList = new LinkedList<>();
      timestampList.add(new ArrayList<>(Arrays.asList(" ", " ")));

      timestampListAll.stream().filter(f -> f.get(2).equals(true))
          .forEach(t -> timestampList.set(0, new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));
      timestampListAll.stream().filter(f -> f.get(2).equals(false))
          .forEach(t -> timestampList.add(new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));

      metadataQueryPanel.getTimestampComboBox().setTableData(timestampList);
      metricQueryPanel.getXTextFile().setText((String) timestampList.get(0).get(0));

      configMetadataCase.getDefaultTableModel().getDataVector().removeAllElements();
      configMetadataCase.getDefaultTableModel().fireTableDataChanged();

      if (tableInfo.getCProfiles() != null) {
        tableInfo.getCProfiles().stream()
            .filter(f -> !f.getCsType().isTimeStamp())
            .forEach(cProfile -> configMetadataCase.getDefaultTableModel().addRow(new Object[]{
                cProfile.getColId(),
                cProfile.getColIdSql(),
                cProfile.getColName(),
                cProfile.getColDbTypeName(),
                cProfile.getCsType().getSType(),
                cProfile.getCsType().getCType()
            }));

        fillTableUI(tableInfo);
      }
    }
  }

  private int getSelectedQueryId() {
    JXTable table = queryCase.getJxTable();
    if (table == null) {
      return -1;
    }

    int selectedRow = table.getSelectedRow();
    if (selectedRow < 0) {
      return -1;
    }

    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    if (tt == null || tt.model() == null) {
      return -1;
    }

    int modelRow = table.convertRowIndexToModel(selectedRow);
    if (modelRow < 0 || modelRow >= tt.model().getRowCount()) {
      return -1;
    }

    QueryRow row = tt.model().itemAt(modelRow);
    return row != null ? row.getId() : -1;
  }

  private void fillTableUI(TableInfo table) {
    metadataQueryPanel.getTableName().setText(table.getTableName());
    metadataQueryPanel.getTableType().setSelectedItem(table.getTableType());
    metadataQueryPanel.getTableIndex().setSelectedItem(table.getIndexType());
    metadataQueryPanel.getCompression().setSelected(Boolean.TRUE.equals(table.getCompression()));
    fillTimestampComboBox(table.getCProfiles());
  }

  private List<String> getProfileNameRunning(int queryID) {
    return profileManager.getProfileInfoList()
        .stream()
        .filter(f -> RunStatus.RUNNING.equals(f.getStatus()))
        .filter(profileInfo -> profileInfo.getTaskInfoList().stream()
            .anyMatch(taskId -> profileManager.getTaskInfoById(taskId).getQueryInfoList().stream()
                .anyMatch(qId -> qId == queryID)))
        .map(ProfileInfo::getName)
        .toList();
  }

  private void loadMetadata() {
    int querySelectedRow = queryCase.getJxTable().getSelectedRow();

    ConnectionInfo connectionInfo = getConnectionInfo();

    if (ConnectionType.JDBC.equals(connectionInfo.getType())) {
      loadMetadataJdbc(connectionInfo);
    } else if (ConnectionType.HTTP.equals(connectionInfo.getType())) {
      loadMetadataHttp(connectionInfo);
    } else {
      loadMetadataJdbc(connectionInfo);
    }

    queryCase.getJxTable().setRowSelectionInterval(querySelectedRow, querySelectedRow);
  }

  private void loadMetadataJdbc(ConnectionInfo connectionInfo) {
    try {
      connectionPoolManager.createDataSource(connectionInfo);
      Connection connection = connectionPoolManager.getConnection(connectionInfo);

      int queryId = getSelectedQueryId();

      QueryInfo queryInfo = getQueryInfo(queryId);
      TableInfo tableInfo = getTableInfo(queryInfo);

      metadataQueryPanel.getTableName().setText(tableInfo.getTableName());

      TProfile tProfile;
      try {
        tProfile = dStore.loadJdbcTableMetadata(connection, queryInfo.getText(), tableInfo.getSProfile());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      List<Metric> metricList = new ArrayList<>();

      queryInfo.setDbType(connectionInfo.getDbType());
      queryInfo.setMetricList(metricList);

      tableInfo.setCProfiles(tProfile.getCProfiles());

      autoSelectTimestampColumn(tableInfo);

      profileManager.updateQuery(queryInfo);
      profileManager.updateTable(tableInfo);

      updateMetadataUI(tableInfo);

      fillTimestampComboBox(tableInfo.getCProfiles());

      updateTableInfo(tableInfo);
      fillConfigMetadata(tableInfo, configMetadataCase);

      publishMetadataUpdate(queryInfo.getId(), queryInfo.getName(), tableInfo.getCProfiles());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void loadMetadataHttp(ConnectionInfo connectionInfo) {
    int queryId = getSelectedQueryId();

    QueryInfo queryInfo = getQueryInfo(queryId);
    TableInfo tableInfo = getTableInfo(queryInfo);

    metadataQueryPanel.getTableName().setText(tableInfo.getTableName());

    TProfile tProfile;
    try {
      SProfile sProfile = new SProfile();
      sProfile.setTableName(tableInfo.getTableName());
      sProfile.setTableType(tableInfo.getTableType() == null ? TType.TIME_SERIES : tableInfo.getTableType());
      sProfile.setIndexType(tableInfo.getIndexType() == null ? IType.LOCAL : tableInfo.getIndexType());
      sProfile.setBackendType(tableInfo.getBackendType() == null ? BType.BERKLEYDB : tableInfo.getBackendType());
      sProfile.setCompression(true);

      fillSProfileFromResponse(exporterParser, httpResponseFetcher.fetchResponse(getHttpProtocol(connectionInfo)), sProfile);
      tProfile = dStore.loadDirectTableMetadata(sProfile);
    } catch (Exception e) {
      log.catching(e);
      throw new RuntimeException(e);
    }

    queryInfo.setDbType(connectionInfo.getDbType());
    queryInfo.setMetricList(new ArrayList<>());

    tableInfo.setTableType(tProfile.getTableType());
    tableInfo.setIndexType(tProfile.getIndexType());
    tableInfo.setBackendType(tProfile.getBackendType());
    tableInfo.setCompression(tProfile.getCompression());
    tableInfo.setCProfiles(tProfile.getCProfiles());

    autoSelectTimestampColumn(tableInfo);

    profileManager.updateQuery(queryInfo);
    profileManager.updateTable(tableInfo);

    updateMetadataUI(tableInfo);

    fillTimestampComboBox(tableInfo.getCProfiles());

    updateTableInfo(tableInfo);

    configMetadataCase.getDefaultTableModel().fireTableDataChanged();

    publishMetadataUpdate(queryInfo.getId(), queryInfo.getName(), tableInfo.getCProfiles());
  }

  private void autoSelectTimestampColumn(TableInfo tableInfo) {
    if (tableInfo.getCProfiles() == null || tableInfo.getCProfiles().isEmpty()) {
      return;
    }

    boolean hasExistingTimestamp = tableInfo.getCProfiles().stream()
        .anyMatch(cp -> cp.getCsType().isTimeStamp());
    if (hasExistingTimestamp) {
      log.debug("Timestamp column already set, skipping auto-selection");
      ensureDefaults(tableInfo);
      return;
    }

    List<CProfile> timestampCandidates = findTimestampColumns(tableInfo.getCProfiles());

    if (timestampCandidates.isEmpty()) {
      log.info("No timestamp candidate columns found for table: {}", tableInfo.getTableName());
      ensureDefaults(tableInfo);
      return;
    }

    TimestampResult result = TimestampColumnChooser.choose(timestampCandidates);

    if (result == null) {
      log.info("User cancelled timestamp column selection for table: {}", tableInfo.getTableName());
      ensureDefaults(tableInfo);
      return;
    }

    CProfile selected = result.column();

    tableInfo.getCProfiles().forEach(cp -> cp.getCsType().setTimeStamp(false));
    selected.getCsType().setTimeStamp(true);

    tableInfo.setCompression(result.compression());

    ensureDefaults(tableInfo);

    log.info("Timestamp column '{}' activated for table '{}', compression={}",
             selected.getColName(), tableInfo.getTableName(), result.compression());
  }

  private void ensureDefaults(TableInfo tableInfo) {
    if (tableInfo.getCompression() == null) {
      tableInfo.setCompression(true);
      log.debug("Compression was null, set to true for table: {}", tableInfo.getTableName());
    }
    if (tableInfo.getTableType() == null) {
      tableInfo.setTableType(TType.TIME_SERIES);
    }
    if (tableInfo.getIndexType() == null) {
      tableInfo.setIndexType(IType.LOCAL);
    }
    if (tableInfo.getBackendType() == null) {
      tableInfo.setBackendType(BType.BERKLEYDB);
    }
  }

  private void updateMetadataUI(TableInfo tableInfo) {
    Runnable uiUpdate = () -> {
      metadataQueryPanel.getTableName().setText(tableInfo.getTableName());
      metadataQueryPanel.getTableType().setSelectedItem(
          tableInfo.getTableType() != null ? tableInfo.getTableType() : TType.TIME_SERIES);
      metadataQueryPanel.getTableIndex().setSelectedItem(
          tableInfo.getIndexType() != null ? tableInfo.getIndexType() : IType.LOCAL);
      metadataQueryPanel.getCompression().setSelected(Boolean.TRUE.equals(tableInfo.getCompression()));
    };

    if (SwingUtilities.isEventDispatchThread()) {
      uiUpdate.run();
    } else {
      SwingUtilities.invokeLater(uiUpdate);
    }
  }

  private List<CProfile> findTimestampColumns(List<CProfile> columns) {
    List<CProfile> result = new ArrayList<>();
    for (CProfile col : columns) {
      if (col.getColDbTypeName() != null && isTimestampType(col.getColDbTypeName())) {
        result.add(col);
      }
    }
    return result;
  }

  private boolean isTimestampType(String typeName) {
    String upper = typeName.toUpperCase().trim();
    return upper.contains("TIMESTAMP") || upper.contains("DATETIME");
  }

  private void publishMetadataUpdate(int queryId, String queryName, List<CProfile> columns) {
    if (columns == null) {
      columns = new ArrayList<>();
    }
    log.info("Publishing UpdateMetadataColumnsEvent for queryId={}, queryName={}, columns={}",
             queryId, queryName, columns.size());
    eventBus.publish(new UpdateMetadataColumnsEvent(queryId, queryName, columns));
  }

  private void fillTimestampComboBox(List<CProfile> cProfileList) {
    List<List<?>> timestampListAll = new LinkedList<>();
    List<List<?>> timestampList = new LinkedList<>();
    timestampList.add(new ArrayList<>(Arrays.asList(" ", " ")));

    if (cProfileList != null) {
      cProfileList.forEach(cProfile -> timestampListAll.add(
          new ArrayList<>(Arrays.asList(cProfile.getColName(), cProfile.getCsType().getSType(), cProfile.getCsType().isTimeStamp()))
      ));
    }

    timestampListAll.stream().filter(f -> f.get(2).equals(true))
        .forEach(t -> timestampList.set(0, new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));
    timestampListAll.stream().filter(f -> f.get(2).equals(false))
        .forEach(t -> timestampList.add(new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));

    metadataQueryPanel.getTimestampComboBox().setTableData(timestampList);
  }

  private ConnectionInfo getConnectionInfo() {
    List<?> rowData = metadataQueryPanel.getQueryConnectionMetadataComboBox().getSelectedRow();
    String connectionName = rowData.get(0).toString();

    return profileManager.getConnectionInfoList().stream()
        .filter(f -> f.getName().equalsIgnoreCase(connectionName))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found connection by name: " + connectionName));
  }

  private void updateTableInfo(TableInfo tableInfo) {
    String timeStampSelected = metadataQueryPanel.getTimestampComboBox().getSelectedItem().toString();

    TTTable<MetadataRow, JXTable> tt = configMetadataCase.getTypedTable();
    List<MetadataRow> metadataRows = new ArrayList<>();
    for (int i = 0; i < tt.model().getRowCount(); i++) {
      metadataRows.add(tt.model().itemAt(i));
    }

    for (MetadataRow metadataRow : metadataRows) {
      if (!metadataRow.hasOrigin()) {
        continue;
      }

      String selectedDataKey = metadataRow.getColName();
      Boolean selectedDimension = metadataRow.isDimension();

      if (Boolean.TRUE.equals(selectedDimension)) {
        if (tableInfo.getDimensionColumnList() == null) {
          tableInfo.setDimensionColumnList(new ArrayList<>());
        }
        if (!tableInfo.getDimensionColumnList().contains(selectedDataKey)) {
          tableInfo.getDimensionColumnList().add(selectedDataKey);
        }
      } else {
        if (tableInfo.getDimensionColumnList() != null) {
          tableInfo.getDimensionColumnList().remove(selectedDataKey);
        }
      }

      String timeStampPrevious = tableInfo.getCProfiles().stream()
          .filter(f -> f.getCsType().isTimeStamp())
          .findAny()
          .orElse(new CProfile().setColName(""))
          .getColName();

      if (!timeStampPrevious.isEmpty() && !timeStampPrevious.equals(timeStampSelected)) {
        tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColName().equals(timeStampPrevious))
            .findAny()
            .orElseThrow()
            .getCsType().setTimeStamp(false);
      }

      if (timeStampSelected.equals(selectedDataKey)) {
        tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColName().equals(selectedDataKey))
            .findAny()
            .orElseThrow()
            .getCsType().setTimeStamp(true);
      }
    }

    if (!timeStampSelected.isBlank()) {
      tableInfo.getCProfiles()
          .stream()
          .filter(f -> f.getColName().equals(timeStampSelected))
          .findAny()
          .orElseThrow()
          .getCsType().setTimeStamp(true);
    }

    fillConfigMetadata(tableInfo, configMetadataCase);
  }

  private QueryInfo getQueryInfo(int queryID) {
    QueryInfo queryInfo = profileManager.getQueryInfoById(queryID);
    if (queryInfo == null) {
      throw new NotFoundException("Not found query: " + queryID);
    }
    return queryInfo;
  }

  private TableInfo getTableInfo(QueryInfo queryInfo) {
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (tableInfo == null) {
      throw new NotFoundException("Not found table: " + queryInfo.getName());
    }
    return tableInfo;
  }

  private void setPanelView(Boolean isSelected) {
    metadataQueryPanel.getConfigMetadataCase().getJxTable().setEditable(!isSelected);
    metadataQueryPanel.getSaveMetadata().setEnabled(!isSelected);
    metadataQueryPanel.getEditMetadata().setEnabled(isSelected);
    metadataQueryPanel.getCancelMetadata().setEnabled(!isSelected);
    metadataQueryPanel.getTimestampComboBox().setEnabled(!isSelected);
    metadataQueryPanel.getTableType().setEnabled(!isSelected);
    metadataQueryPanel.getTableIndex().setEnabled(!isSelected);
    metadataQueryPanel.getCompression().setEnabled(!isSelected);

    mainQuery.setEnabledAt(0, isSelected);
    mainQuery.setEnabledAt(2, isSelected);

    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(0, isSelected);

    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);

    checkboxConfig.setEnabled(isSelected);
  }
}