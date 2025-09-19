package ru.dimension.ui.view.handler.query;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.collector.HttpLoader;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.handler.CommonViewHandler;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;


@Log4j2
@Singleton
public class QueryMetadataHandler implements ActionListener, CommonViewHandler, HttpLoader {

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final ProfileManager profileManager;
  private final ConnectionPoolManager connectionPoolManager;
  private final JXTableCase configMetadataCase;

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
                              @Named("jTabbedPaneConfig") ConfigTab configTab,
                              @Named("exporterParser") ExporterParser exporterParser,
                              @Named("httpResponseFetcher") HttpResponseFetcher httpResponseFetcher,
                              @Named("localDB") DStore dStore) {
    this.profileManager = profileManager;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.connectionPoolManager = connectionPoolManager;
    this.configMetadataCase = configMetadataCase;

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
    ListSelectionModel listSelectionModel = this.queryCase.getJxTable().getSelectionModel();

    int queryID = getSelectedQueryId(listSelectionModel);
    if (queryID > 0) {
      QueryInfo query = profileManager.getQueryInfoById(queryID);
      if (Objects.isNull(query)) {
        throw new NotFoundException("Not found query: " + queryID);
      }
      TableInfo table = profileManager.getTableInfoByTableName(query.getName());
      if (Objects.isNull(table)) {
        throw new NotFoundException("Not found table: " + query.getName());
      }
      List<String> profileNameRunning = getProfileNameRunning(queryID);
      if (e.getSource() == metadataQueryPanel.getLoadMetadata()) {
        if (queryCase.getJxTable().getSelectedRow() == -1) {
          throw new NotSelectedRowException("The query is not selected. Please select and try again!");
        } else {

          if (!profileNameRunning.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                                          "You can't load metadata for query " + query.getName()
                                              + " used by running profiles: "
                                              + String.join(",", profileNameRunning), "Error", JOptionPane.ERROR_MESSAGE);
          } else {
            if (table.getCProfiles() != null) {
              int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                        "Do you want to rewrite existing metadata for query "
                                                            + query.getName() + " ?");
              if (input == 0) {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                  try {
                    loadMetadata();
                  } catch (Exception ex) {
                    ex.printStackTrace();
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
                  } else {
                    throw new RuntimeException(cause);
                  }
                });

                future.thenRun(() -> {
                  JOptionPane.showMessageDialog(null, "Metadata download completed",
                                                "Information", JOptionPane.INFORMATION_MESSAGE);
                });
              }
            } else {
              int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                        "Do you want to get metadata for query " + query.getName()
                                                            + " ?");
              if (input == 0) {
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
                  } else {
                    throw new RuntimeException(cause);
                  }
                });

                future.thenRun(() -> {
                  JOptionPane.showMessageDialog(null, "Metadata download completed",
                                                "Information", JOptionPane.INFORMATION_MESSAGE);
                });
              }
            }
          }
        }
      }

      if (e.getSource() == metadataQueryPanel.getEditMetadata()) {
        setPanelView(false);
      }

      if (e.getSource() == metadataQueryPanel.getSaveMetadata()) {
        setPanelView(true);

        QueryInfo queryInfo = getQueryInfo(query.getId());
        TableInfo tableInfo = getTableInfo(queryInfo);

        if (Objects.isNull(tableInfo.getDimensionColumnList())) {
          tableInfo.setDimensionColumnList(new ArrayList<>());
        }

        // Fill SProfile configuration from UI
        tableInfo.setTableType(TType.valueOf(
            Objects.requireNonNull(metadataQueryPanel.getTableType().getSelectedItem()).toString()));
        tableInfo.setIndexType(IType.valueOf(
            Objects.requireNonNull(metadataQueryPanel.getTableIndex().getSelectedItem()).toString()));
        tableInfo.setCompression(metadataQueryPanel.getCompression().isSelected());
        tableInfo.setBackendType(BType.BERKLEYDB);

        ConnectionInfo connectionInfo = getConnectionInfo();
        try {
          connectionPoolManager.createDataSource(connectionInfo);
          java.sql.Connection connection = connectionPoolManager.getConnection(connectionInfo);

          TProfile tProfile = dStore.loadJdbcTableMetadata(connection, queryInfo.getText(), tableInfo.getSProfile());

          tableInfo.setCProfiles(tProfile.getCProfiles());
        } catch (SQLException | TableNameEmptyException ex) {
          throw new RuntimeException(ex);
        }

        updateTableInfo(tableInfo);

        profileManager.updateQuery(queryInfo);
        profileManager.updateTable(tableInfo);

        fillTableUI(tableInfo);
      }

      if (e.getSource() == metadataQueryPanel.getCancelMetadata()) {

        if (queryCase.getJxTable().getSelectedRowCount() > 0) {
          int queryId = (Integer) queryCase.getDefaultTableModel()
              .getValueAt(queryCase.getJxTable().getSelectedRow(), 0);

          setPanelView(true);

          QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
          if (Objects.isNull(queryInfo)) {
            throw new NotFoundException("Not found query: " + queryId);
          }
          TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
          List<List<?>> connectionData = new LinkedList<>();
          profileManager.getTaskInfoList().stream()
              .filter(f -> f.getQueryInfoList().stream().anyMatch(qId -> qId == queryId))
              .findAny()
              .ifPresentOrElse(t -> {
                ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(t.getConnectionId());
                if (Objects.isNull(connectionInfo)) {
                  throw new NotFoundException("Not found task: " + t.getConnectionId());
                }
                connectionData.add(
                    new ArrayList<>(Arrays.asList(connectionInfo.getName(),
                                                  connectionInfo.getUserName(),
                                                  connectionInfo.getUrl(),
                                                  connectionInfo.getJar(),
                                                  connectionInfo.getDriver(),
                                                  connectionInfo.getType())));
              }, () -> log.info("Not found query by query id: " + queryId));

          metadataQueryPanel.getQueryConnectionMetadataComboBox().setTableData(connectionData);

          if (tableInfo.getTableType() != null) {
            metadataQueryPanel.getTableType().setSelectedItem(tableInfo.getTableType());
          } else {
            metadataQueryPanel.getTableType().setSelectedItem(TType.TIME_SERIES);
          }
          if (tableInfo.getIndexType() != null) {
            metadataQueryPanel.getTableIndex().setSelectedItem(tableInfo.getIndexType());
          } else {
            metadataQueryPanel.getTableIndex().setSelectedItem(IType.LOCAL);
          }

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
          if (table != null && table.getCProfiles() != null) {
            table.getCProfiles().stream()
                .filter(f -> !f.getCsType().isTimeStamp())
                .forEach(cProfile -> configMetadataCase.getDefaultTableModel()
                    .addRow(new Object[]{cProfile.getColId(), cProfile.getColIdSql(), cProfile.getColName(),
                        cProfile.getColDbTypeName(), cProfile.getCsType().getSType(),
                        cProfile.getCsType().getCType()
                    }));

            fillTableUI(table);
          }
        }
      }
    } else {
      throw new NotSelectedRowException("Not selected query. Please select and try again!");
    }
  }

  private int getSelectedQueryId(ListSelectionModel listSelectionModel) {
    return GUIHelper.getIdByColumnName(queryCase.getJxTable(),
                                       queryCase.getDefaultTableModel(),
                                       listSelectionModel, TaskColumnNames.ID.getColName());
  }

  private void fillTableUI(TableInfo table) {
    metadataQueryPanel.getTableName().setText(table.getTableName());
    metadataQueryPanel.getTableType().setSelectedItem(table.getTableType());
    metadataQueryPanel.getTableIndex().setSelectedItem(table.getIndexType());
    metadataQueryPanel.getCompression().setSelected(table.getCompression());

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

    log.info(connectionInfo.getType());
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
      java.sql.Connection connection = connectionPoolManager.getConnection(connectionInfo);

      int queryId = getSelectedQueryId(this.queryCase.getJxTable().getSelectionModel());

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

      profileManager.updateQuery(queryInfo);
      profileManager.updateTable(tableInfo);

      fillTimestampComboBox(tableInfo.getCProfiles());

      updateTableInfo(tableInfo);

      configMetadataCase.getDefaultTableModel().fireTableDataChanged();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void loadMetadataHttp(ConnectionInfo connectionInfo) {
    int queryId = getSelectedQueryId(this.queryCase.getJxTable().getSelectionModel());

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
      throw new RuntimeException(e);
    }

    queryInfo.setDbType(connectionInfo.getDbType());
    queryInfo.setMetricList(new ArrayList<>());

    tableInfo.setTableType(tProfile.getTableType());
    tableInfo.setIndexType(tProfile.getIndexType());
    tableInfo.setBackendType(tProfile.getBackendType());
    tableInfo.setCompression(tProfile.getCompression());
    tableInfo.setCProfiles(tProfile.getCProfiles());

    profileManager.updateQuery(queryInfo);
    profileManager.updateTable(tableInfo);

    fillTimestampComboBox(tableInfo.getCProfiles());

    updateTableInfo(tableInfo);

    configMetadataCase.getDefaultTableModel().fireTableDataChanged();
  }

  private void fillTimestampComboBox(List<CProfile> cProfileList) {
    List<List<?>> timestampListAll = new LinkedList<>();
    List<List<?>> timestampList = new LinkedList<>();
    timestampList.add(new ArrayList<>(Arrays.asList(" ", " ")));

    if (cProfileList != null) {
      cProfileList.forEach(cProfile -> timestampListAll.add(
          new ArrayList<>(Arrays.asList(cProfile.getColName(), cProfile.getCsType().getSType(),
                                        cProfile.getCsType().isTimeStamp()))));
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

    for (int i = 0; i < configMetadataCase.getDefaultTableModel().getRowCount(); i++) {
      String selectedDataKey = (String) configMetadataCase.getDefaultTableModel().getValueAt(i, 2);
      SType selectedDataSType = (SType) configMetadataCase.getDefaultTableModel().getValueAt(i, 4);

      Boolean selectedDimension = (Boolean) configMetadataCase.getDefaultTableModel().getValueAt(i, 6);

      if (Boolean.TRUE.equals(selectedDimension)) {
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
            .getCsType().setSType(selectedDataSType);
        tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColName().equals(selectedDataKey))
            .findAny()
            .orElseThrow()
            .getCsType().setTimeStamp(true);
      } else {
        tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColName().equals(selectedDataKey))
            .findAny()
            .orElseThrow()
            .getCsType().setSType(selectedDataSType);
        tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColName().equals(selectedDataKey))
            .findAny()
            .orElseThrow()
            .getCsType().setTimeStamp(false);
      }
    }

    if (!timeStampSelected.isBlank()) {
      tableInfo.getCProfiles()
          .stream()
          .filter(f -> f.getColName().equals(timeStampSelected))
          .findAny()
          .orElseThrow().getCsType().setTimeStamp(true);
    }

    configMetadataCase.getDefaultTableModel().getDataVector().removeAllElements();
    configMetadataCase.getDefaultTableModel().fireTableDataChanged();

    if (tableInfo != null && tableInfo.getCProfiles() != null) {
      fillConfigMetadata(tableInfo, configMetadataCase);
    }
  }

  private QueryInfo getQueryInfo(int queryID) {
    QueryInfo queryInfo = profileManager.getQueryInfoById(queryID);
    if (Objects.isNull(queryInfo)) {
      throw new NotFoundException("Not found query: " + queryID);
    }

    return queryInfo;
  }

  private TableInfo getTableInfo(QueryInfo queryInfo) {
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (Objects.isNull(tableInfo)) {
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

