package ru.dimension.ui.view.structure.workspace.handler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.state.GUIState;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.QueryMetadataColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;

@Log4j2
public class MetricColumnSelectionHandler extends ChartHandler
    implements ActionListener {

  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;

  private final DetailsControlPanelHandler detailsControlPanelHandler;

  private final DetailsControlPanel detailsControlPanel;

  private final GUIState guiState = GUIState.getInstance();

  public MetricColumnSelectionHandler(TaskTab taskTab,
                                      RealTimeTab realTimeTab,
                                      HistoryTab historyTab,
                                      ProfileTaskQueryKey profileTaskQueryKey,
                                      QueryInfo queryInfo,
                                      TableInfo tableInfo,
                                      ChartInfo chartInfo,
                                      JXTableCase jxTableCaseMetrics,
                                      JXTableCase jxTableCaseColumns,
                                      JSplitPane visualizeRealTime,
                                      JSplitPane visualizeHistory,
                                      JPanel analyzeRealTime,
                                      JPanel analyzeHistory,
                                      DetailsControlPanel detailsControlPanel,
                                      DetailsControlPanelHandler detailsControlPanelHandler,
                                      WorkspaceQueryComponent workspaceQueryComponent) {
    super(taskTab, realTimeTab, historyTab, jxTableCaseMetrics, jxTableCaseColumns, tableInfo, queryInfo, chartInfo,
          profileTaskQueryKey, visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, detailsControlPanel, workspaceQueryComponent);

    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;

    this.detailsControlPanel = detailsControlPanel;
    this.detailsControlPanelHandler = detailsControlPanelHandler;

    this.detailsControlPanel.getButtonGroupFunction().getCount().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getSum().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getAverage().addActionListener(new RadioListenerColumn());

    this.detailsControlPanel.getSaveButton().addActionListener(this);
    this.detailsControlPanel.getCancelButton().addActionListener(this);

    // Initialize TableSelectionHandler for metrics table
    new TableSelectionHandler(
        jxTableCaseMetrics,
        MetricsColumnNames.NAME.getColName(),
        id -> {
          int metricId = GUIHelper.getIdByColumnName(jxTableCaseMetrics.getJxTable(),
                                                     jxTableCaseMetrics.getDefaultTableModel(),
                                                     jxTableCaseMetrics.getJxTable().getSelectionModel(),
                                                     MetricsColumnNames.ID.getColName());
          this.setSourceConfig(jxTableCaseMetrics.getJxTable());
          this.loadMetric(metricId);
          this.loadChart(guiState.getTaskTabState());
        }
    );

    // Initialize TableSelectionHandler for columns table
    new TableSelectionHandler(
        jxTableCaseColumns,
        QueryMetadataColumnNames.NAME.getColName(),
        id -> {
          int cProfileId = GUIHelper.getIdByColumnName(jxTableCaseColumns.getJxTable(),
                                                       jxTableCaseColumns.getDefaultTableModel(),
                                                       jxTableCaseColumns.getJxTable().getSelectionModel(),
                                                       QueryMetadataColumnNames.ID.getColName());
          this.setSourceConfig(jxTableCaseColumns.getJxTable());
          this.loadColumn(cProfileId);
          this.loadChart(guiState.getTaskTabState());
        }
    );
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
    CProfile cProfile = tableInfo.getCProfiles().stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow();

    log.info(cProfile);

    checkStatusAndDoAction();

    detailsControlPanelHandler.clearAll();
    detailsControlPanelHandler.loadColumnToDetails(getMetricByCProfile(cProfile));
  }

  private void checkStatusAndDoAction() {
    if (detailsControlPanelHandler.getStatus().equals(LifeCycleStatus.EDIT)) {
      detailsControlPanelHandler.cancelToSaveNewMetric();
      metricFunctionOnEdit = MetricFunction.NONE;
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == detailsControlPanel.getSaveButton()) {
      log.info("Cancel button clicked");
      metricFunctionOnEdit = MetricFunction.NONE;
    } else if (e.getSource() == detailsControlPanel.getCancelButton()) {
      log.info("Cancel button clicked");
      metricFunctionOnEdit = MetricFunction.NONE;
    }
  }

  private class RadioListenerColumn implements ActionListener {

    public RadioListenerColumn() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();

      RadioButtonActionExecutor.execute(button, metricFunction -> {
        metricFunctionOnEdit = metricFunction;
        loadChart(guiState.getTaskTabState());
      });
    }
  }
}