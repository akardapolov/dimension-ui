package ru.dimension.ui.view.handler.adhoc;

import static ru.dimension.ui.model.view.ProcessType.ADHOC;
import static ru.dimension.ui.model.view.ProcessTypeWorkspace.VISUALIZE;
import static ru.dimension.ui.model.view.RangeHistory.DAY;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.column.QueryMetadataColumnNames;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.panel.adhoc.AdHocPanel;
import ru.dimension.ui.view.structure.workspace.handler.DetailsControlPanelHandler;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;
import ru.dimension.ui.view.panel.RangeChartHistoryPanel;

@Log4j2
@Singleton
public class AdHocSelectionHandler extends AdHocChartHandler implements ActionListener, HelperChart {

  private final ProfileManager profileManager;
  private final AdHocDatabaseManager adHocDatabaseManager;
  private final AdHocPanel adHocPanel;
  private final RangeChartHistoryPanel rangeChartHistoryPanel;
  private final JXTableCase connectionCase;
  private final JComboBox<String> schemaCatalogCBox;
  private final JXTableCase tableCase;
  private final JXTableCase viewCase;
  private final ConnectionPoolManager connectionPoolManager;
  private final JXTableCase metricCase;
  private final JXTitledSeparator tableNameAdHocTitle;
  private final JXTableCase timestampCase;
  private final JXTableCase columnCase;
  private TProfile tProfile;
  private DStore dStore;
  private final DetailsControlPanelHandler detailsControlPanelHandler;

  @Inject
  public AdHocSelectionHandler(@Named("adHocPanel") AdHocPanel adHocPanel,
                               @Named("profileManager") ProfileManager profileManager,
                               @Named("adHocDatabaseManager") AdHocDatabaseManager adHocDatabaseManager,
                               @Named("connectionAdHocCase") JXTableCase connectionCase,
                               @Named("schemaCatalogAdHocCBox") JComboBox<String> schemaCatalogCBox,
                               @Named("tableAdHocCase") JXTableCase tableCase,
                               @Named("viewAdHocCase") JXTableCase viewCase,
                               @Named("metricAdHocCase") JXTableCase metricCase,
                               @Named("tableNameAdHocTitle") JXTitledSeparator tableNameAdHocTitle,
                               @Named("timestampAdHocCase") JXTableCase timestampCase,
                               @Named("columnAdHocCase") JXTableCase columnCase,
                               @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager) {
    super(adHocPanel, metricCase, timestampCase, columnCase, connectionCase, tableCase);

    this.profileManager = profileManager;
    this.adHocDatabaseManager = adHocDatabaseManager;
    this.detailsControlPanelHandler = adHocPanel.getDetailsControlPanelHandler();

    this.adHocPanel = adHocPanel;
    this.connectionCase = connectionCase;
    this.schemaCatalogCBox = schemaCatalogCBox;
    this.tableCase = tableCase;
    this.viewCase = viewCase;
    this.metricCase = metricCase;
    this.columnCase = columnCase;
    this.tableNameAdHocTitle = tableNameAdHocTitle;
    this.timestampCase = timestampCase;
    this.connectionPoolManager = connectionPoolManager;

    this.chartInfo.setCustomEnd(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    this.chartInfo.setRangeHistory(DAY);

    this.rangeChartHistoryPanel = adHocPanel.getRangeChartHistoryPanel();
    this.rangeChartHistoryPanel.addActionListener(this);

    this.detailsControlPanel.getButtonGroupFunction().getCount().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getSum().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getAverage().addActionListener(new RadioListenerColumn());

    Consumer<String> runActionTable = this::runAction;
    new TableSelectionHandler(this.tableCase, ConnectionColumnNames.NAME.getColName(), runActionTable, schemaCatalogCBox);

    Consumer<String> runActionView = this::runAction;
    new TableSelectionHandler(this.viewCase, ConnectionColumnNames.NAME.getColName(), runActionView, schemaCatalogCBox);

    Consumer<String> runActionMetric = this::runActionMetric;
    new TableSelectionHandler(this.metricCase, ConnectionColumnNames.NAME.getColName(), runActionMetric);

    Consumer<String> runActionColumn = this::runActionColumn;
    new TableSelectionHandler(this.columnCase, ConnectionColumnNames.NAME.getColName(), runActionColumn);

    Consumer<String> runActionTimestamp = this::runActionTimestamp;
    new TableSelectionHandler(this.timestampCase, ConnectionColumnNames.NAME.getColName(), runActionTimestamp);
  }

  private void runActionMetric(String metricName) {
    log.info("Run action for metric: " + metricName);

    int metricId = GUIHelper.getIdByColumnName(metricCase, ConnectionColumnNames.ID.getColName());

    this.setSourceConfig(metricCase.getJxTable());
    this.loadMetric(metricId);

    this.loadChart(ADHOC, dStore, tableInfo, queryInfo);
  }

  private void runActionColumn(String columnName) {
    log.info("Run action for column: " + columnName);

    if (columnName.isBlank()) return;

    if (adHocPanel.isClearFlag()) {
      return;
    }

    if (isTimeStampColumnEmptyOrNotSelected()) {
      return;
    }

    int cProfileId = GUIHelper.getIdByColumnName(columnCase, ConnectionColumnNames.ID.getColName());

    this.setSourceConfig(columnCase.getJxTable());
    this.loadColumn(cProfileId);

    this.loadChart(ADHOC, dStore, tableInfo, queryInfo);
  }

  private void runActionTimestamp(String timestampName) {
    log.info("Run action for timestamp: " + timestampName);

    if (timestampName.isBlank()) return;

    if (adHocPanel.isClearFlag()) {
      return;
    }

    setTimestampColumn(timestampName);

    if (isColumnEmptyOrNotSelected()) return;

    this.loadChart(ADHOC, dStore, tableInfo, queryInfo);
  }

  private void setTimestampColumn(String timestampName) {
    try {
      dStore.setTimestampColumn(tProfile.getTableName(), timestampName);

      tProfile = dStore.getTProfile(tProfile.getTableName());

      tableInfo = new TableInfo(tProfile);
    } catch (TableNameEmptyException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isColumnEmptyOrNotSelected() {
    if (columnCase.getJxTable().getRowCount() == 0) {
      DialogHelper.showMessageDialog(null,
                                     "Columns list is empty. Please try another table or view",
                                     "Warning");
      return true;
    }

    String columnName = GUIHelper.getNameByColumnName(columnCase.getJxTable(),
                                                      columnCase.getDefaultTableModel(),
                                                      columnCase.getJxTable().getSelectionModel(),
                                                      QueryMetadataColumnNames.NAME.getColName());

    if (columnName.isBlank()) {
      DialogHelper.showMessageDialog(null,
                                     "Column not selected, please select it",
                                     "Warning");
      return true;
    }

    return false;
  }

  private boolean isTimeStampColumnEmptyOrNotSelected() {
    if (timestampCase.getJxTable().getRowCount() == 0) {
      DialogHelper.showMessageDialog(null,
                                     "Timestamp columns list is empty. Please try another table or view",
                                     "Warning");
      return true;
    }

    String tsColumnName = GUIHelper.getNameByColumnName(timestampCase.getJxTable(),
                                                        timestampCase.getDefaultTableModel(),
                                                        timestampCase.getJxTable().getSelectionModel(),
                                                        QueryMetadataColumnNames.NAME.getColName());

    if (tsColumnName.isBlank()) {
      DialogHelper.showMessageDialog(null,
                                     "Timestamp column not selected, please pick it",
                                     "Warning");
      return true;
    }

    return false;
  }

  private void loadMetric(int metricId) {
    log.info("Load metrics..");
    Metric metric = queryInfo.getMetricList() == null ? new Metric() : queryInfo.getMetricList().stream()
        .filter(f -> f.getId() == metricId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found metric by id: " + metricId));

    log.info(metric);

    checkStatusAndDoAction();

    detailsControlPanelHandler.clearAll();
    detailsControlPanelHandler.loadMetricToDetails(metric);
  }

  private void loadColumn(int cProfileId) {
    log.info("Load columns..");
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();

    log.info(cProfile);

    checkStatusAndDoAction();

    detailsControlPanelHandler.clearAll();
    detailsControlPanelHandler.loadColumnToDetails(getMetricByCProfile(cProfile, tableInfo));
  }

  private void checkStatusAndDoAction() {
    if (detailsControlPanelHandler.getStatus().equals(LifeCycleStatus.EDIT)) {
      /**
       TODO Save metric functions implementation in progress
       JTextFieldCase jTextFieldCase = GUIHelper.getJTextFieldCase("Metric name");

       int input = JOptionPane.showOptionDialog(null,
       jTextFieldCase.getJPanel(),"Create new metric?",
       JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,null,
       new String[]{"Yes", "No"},"No");

       if (input == 0) {
       detailsControlPanelHandler.saveNewMetric(jTextFieldCase.getJTextField().getText());
       } else if (input == 1) {
       detailsControlPanelHandler.cancelToSaveNewMetric();
       }
       **/

      detailsControlPanelHandler.cancelToSaveNewMetric();

      metricFunctionOnEdit = MetricFunction.NONE;
    }
  }

  private void runAction(String tableName) {
    log.info("Run action for table: " + tableName);

    detailsControlPanelHandler.clearAll();

    cleanAllPanels();

    if (tableName.isEmpty()) {
      return;
    }

    loadTableView(tableName);
  }

  private void loadTableView(String tableName) {
    log.info("Table/view name: " + tableName);
    tableNameAdHocTitle.setTitle("Table/view name: " + tableName + " is loading..");

    int connectionId =
        GUIHelper.getIdByColumnName(connectionCase, ConnectionColumnNames.ID.getColName());

    Connection connection;
    try {
      ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connectionId);

      connection = connectionPoolManager.getConnection(connectionInfo);

      String queryText = "SELECT * FROM " + tableName + " LIMIT 1";

      if (DBType.ORACLE.equals(connectionInfo.getDbType())) {
        queryText = "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
      } else if (DBType.MSSQL.equals(connectionInfo.getDbType())) {
        queryText = "SELECT TOP 1 * FROM " + tableName;
      }

      dStore = adHocDatabaseManager.getDataBase(connectionInfo);

      // TODO BUG: run load jdbc metadata (and loadTableView too) in own thread (for long running query)
      //  cause incomplete metadata saving when call setTimestampColumn
      //  How to resolve: call query to get metadata directly for each type of DB (possible do it in Dimension DB)
      //  (see processEntitiesFilteredOracle in AdHocPresenter)
      log.info("Start SQL query: " + queryText);
      tProfile = dStore.loadJdbcTableMetadata(connection, queryText,
                                              getSProfile(tableName, connectionInfo.getDbType()));
      log.info("Stop SQL query: " + queryText);

      log.info(tProfile);

      if (tProfile.getCProfiles() == null) {
        throw new RuntimeException("Error while loading metadata for table name: " + tableName);
      }

      tableInfo = new TableInfo(tProfile);
      tableInfo.setDimensionColumnList(new ArrayList<>());

      queryInfo.setId(tableName.hashCode());
      queryInfo.setName(tableName);
      queryInfo.setText(queryText);

      chartInfo.setId(queryInfo.getId());

      profileManager.updateChart(chartInfo);

      columnCase.getDefaultTableModel().getDataVector().removeAllElements();
      columnCase.getDefaultTableModel().fireTableDataChanged();
      timestampCase.getDefaultTableModel().getDataVector().removeAllElements();
      timestampCase.getDefaultTableModel().fireTableDataChanged();

      List<CProfile> cProfileList = tProfile.getCProfiles();
      for (CProfile cProfile : cProfileList) {
        switch (cProfile.getCsType().getDType()) {
          case TIMESTAMP, TIMESTAMPTZ, TIMETZ, TIME, DATETIME, SMALLDATETIME, DATE, DATE32, DATETIME2 ->
              timestampCase.getDefaultTableModel().addRow(new Object[]{cProfile.getColName()});
          default -> columnCase.getDefaultTableModel().addRow(new Object[]{cProfile.getColId(), cProfile.getColName()});
        }
      }

      if (timestampCase.getJxTable().getRowCount() == 0) {
        tableNameAdHocTitle.setTitle("Table name: not selected");
        columnCase.getDefaultTableModel().getDataVector().removeAllElements();
        columnCase.getDefaultTableModel().fireTableDataChanged();

        DialogHelper.showMessageDialog(null,
                                       "Timestamp column does not exist. Please try another table or view",
                                       "Warning");
      } else {
        timestampCase.setBlockRunAction(true);
        timestampCase.getJxTable().setRowSelectionInterval(0, 0);

        String tsColumnName = GUIHelper.getNameByColumnName(timestampCase.getJxTable(),
                                                            timestampCase.getDefaultTableModel(),
                                                            timestampCase.getJxTable().getSelectionModel(),
                                                            QueryMetadataColumnNames.NAME.getColName());

        setTimestampColumn(tsColumnName);

        timestampCase.setBlockRunAction(false);
      }

      tableNameAdHocTitle.setTitle("Table/view name: " + tableName);

    } catch (Exception e) {
      tableNameAdHocTitle.setTitle("Table name: not selected");

      columnCase.getDefaultTableModel().getDataVector().removeAllElements();
      columnCase.getDefaultTableModel().fireTableDataChanged();
      timestampCase.getDefaultTableModel().getDataVector().removeAllElements();
      timestampCase.getDefaultTableModel().fireTableDataChanged();

      throw new RuntimeException(e);
    }
  }

  public SProfile getSProfile(String tableName,
                              DBType dbType) {
    SProfile sProfile = new SProfile();
    sProfile.setTableName(tableName);
    sProfile.setTableType(TType.TIME_SERIES);
    sProfile.setIndexType(IType.GLOBAL);
    sProfile.setCompression(true);
    sProfile.setCsTypeMap(new HashMap<>());

    if (dbType.equals(DBType.CLICKHOUSE)) {
      sProfile.setBackendType(BType.CLICKHOUSE);
    } else if (dbType.equals(DBType.POSTGRES)) {
      sProfile.setBackendType(BType.POSTGRES);
    } else if (dbType.equals(DBType.ORACLE)) {
      sProfile.setBackendType(BType.ORACLE);
    } else if (dbType.equals(DBType.MSSQL)) {
      sProfile.setBackendType(BType.MSSQL);
    } else if (dbType.equals(DBType.MYSQL)) {
      sProfile.setBackendType(BType.MYSQL);
    } else if (dbType.equals(DBType.DUCKDB)) {
      sProfile.setBackendType(BType.DUCKDB);
    }

    return sProfile;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("Action command here: " + e.getActionCommand());
    String name = e.getActionCommand().replace("Last ", "");

    Stream.of(RangeHistory.values())
        .filter(v -> v.getName().equals(name))
        .forEach(this.chartInfo::setRangeHistory);

    String columnName = GUIHelper.getNameByColumnName(columnCase.getJxTable(),
                                                      columnCase.getDefaultTableModel(),
                                                      columnCase.getJxTable().getSelectionModel(),
                                                      QueryMetadataColumnNames.NAME.getColName());

    if (columnName.isBlank()) {
      DialogHelper.showMessageDialog(null,
                                     "Please select a column to continue",
                                     "Warning");

      return;
    }

    if (Stream.of(RangeHistory.values()).anyMatch(v -> v.getName().equals(name))) {
      adHocPanel.getAdHocTab().setSelectedTab(VISUALIZE);
      colorButtonHistory(name);

      loadChart(ADHOC, dStore, tableInfo, queryInfo);
    }

    if (e.getSource() == detailsControlPanel.getSaveButton()) {
      log.info("Cancel button clicked");
      metricFunctionOnEdit = MetricFunction.NONE;
    } else if (e.getSource() == detailsControlPanel.getCancelButton()) {
      log.info("Cancel button clicked");
      metricFunctionOnEdit = MetricFunction.NONE;
    }
  }

  private void colorButtonHistory(String name) {
    switch (name) {
      case "Day" -> this.rangeChartHistoryPanel.setButtonColor(colorBlack, colorBlue, colorBlue, colorBlue);
      case "Week" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlack, colorBlue, colorBlue);
      case "Month" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlue, colorBlack, colorBlue);
      case "Custom" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlue, colorBlue, colorBlack);
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    log.info("Mouse clicked");
  }

  private class RadioListenerColumn implements ActionListener {

    public RadioListenerColumn() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();

      RadioButtonActionExecutor.execute(button, metricFunction -> {
        metricFunctionOnEdit = metricFunction;
        loadChart(ADHOC, dStore, tableInfo, queryInfo);
      });
    }
  }
}

