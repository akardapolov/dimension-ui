package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.model.SourceConfig.COLUMNS;
import static ru.dimension.ui.model.SourceConfig.METRICS;
import static ru.dimension.ui.model.view.ProcessType.HISTORY;
import static ru.dimension.ui.model.view.ProcessType.HISTORY_ANALYZE;
import static ru.dimension.ui.model.view.ProcessType.REAL_TIME;
import static ru.dimension.ui.model.view.ProcessType.REAL_TIME_ANALYZE;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.state.GUIState;
import ru.dimension.ui.view.analyze.CustomAction;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.analyze.panel.AnalyzeHistoryPanel;
import ru.dimension.ui.view.analyze.panel.AnalyzeRealtimePanel;
import ru.dimension.ui.view.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.view.chart.stacked.ClientRealTimeSCP;
import ru.dimension.ui.view.chart.stacked.ServerClientHistorySCP;
import ru.dimension.ui.view.chart.stacked.ServerRealTimeSCP;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;
import ru.dimension.ui.view.detail.DetailPanel;
import ru.dimension.ui.view.handler.MouseListenerImpl;

@Log4j2
public abstract class ChartHandler extends MouseListenerImpl implements HelperChart {

  protected TaskTab taskTab;
  protected RealTimeTab realTimeTab;
  protected HistoryTab historyTab;

  protected QueryInfo queryInfo;
  private final TableInfo tableInfo;

  protected ChartInfo chartInfo;
  protected SourceConfig sourceConfig = METRICS;

  protected final JXTableCase jxTableCaseMetrics;
  protected final JXTableCase jxTableCaseColumns;

  protected WorkspaceQueryComponent workspaceQueryComponent;
  protected ProfileTaskQueryKey profileTaskQueryKey;

  protected JSplitPane visualizeRealTime;
  protected JSplitPane visualizeHistory;
  protected JPanel analyzeRealTime;
  protected JPanel analyzeHistory;

  protected ExecutorService executorService;

  protected final ResourceBundle bundleDefault;

  protected Color colorBlack;
  protected Color colorBlue;

  protected MetricFunction metricFunctionOnEdit = MetricFunction.NONE;

  protected DetailsControlPanel detailsControlPanel;

  private final GUIState guiState = GUIState.getInstance();

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  public ChartHandler(TaskTab taskTab,
                      RealTimeTab realTimeTab,
                      HistoryTab historyTab,
                      JXTableCase jxTableCaseMetrics,
                      JXTableCase jxTableCaseColumns,
                      TableInfo tableInfo,
                      QueryInfo queryInfo,
                      ChartInfo chartInfo,
                      ProfileTaskQueryKey profileTaskQueryKey,
                      JSplitPane visualizeRealTime,
                      JSplitPane visualizeHistory,
                      JPanel analyzeRealTime,
                      JPanel analyzeHistory,
                      DetailsControlPanel detailsControlPanel,
                      WorkspaceQueryComponent workspaceQueryComponent) {
    this.taskTab = taskTab;
    this.realTimeTab = realTimeTab;
    this.historyTab = historyTab;
    this.jxTableCaseMetrics = jxTableCaseMetrics;
    this.jxTableCaseColumns = jxTableCaseColumns;
    this.tableInfo = tableInfo;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.visualizeRealTime = visualizeRealTime;
    this.visualizeHistory = visualizeHistory;
    this.analyzeRealTime = analyzeRealTime;
    this.analyzeHistory = analyzeHistory;
    this.detailsControlPanel = detailsControlPanel;
    this.workspaceQueryComponent = workspaceQueryComponent;
    this.executorService = Executors.newSingleThreadExecutor();

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.colorBlack = Color.BLACK;
    this.colorBlue = (Color) bundleDefault.getObject("colorBlue");

    this.detailsControlPanel.getButtonGroupFunction().getCount().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getSum().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getAverage().addActionListener(new RadioListenerColumn());

    this.jxTableCaseMetrics.getJxTable().addMouseListener(this);
    this.jxTableCaseColumns.getJxTable().addMouseListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    JXTable source = (JXTable) e.getSource();

    this.setSourceConfig(source);

    log.info("Source config: " + sourceConfig);
  }

  protected void setSourceConfig(JXTable source) {
    if (source.equals(jxTableCaseMetrics.getJxTable())) {
      sourceConfig = METRICS;
    }

    if (source.equals(jxTableCaseColumns.getJxTable())) {
      sourceConfig = COLUMNS;
    }

    log.info("Source config: " + sourceConfig);
  }

  protected void loadChart(ProcessType processType) {
    log.info("Load chart method call");
    this.taskTab.setSelectedTab(processType);

    if (REAL_TIME.equals(processType)) {
      this.realTimeTab.setSelectedTab(guiState.getRealTimeTabState());
    } else {
      this.historyTab.setSelectedTab(guiState.getHistoryTabState());
    }

    this.checkTimestampColumn();

    if (METRICS.equals(sourceConfig)) {
      this.loadChartMetric(processType);
    } else if (COLUMNS.equals(sourceConfig)) {
      this.loadChartColumn(processType);
    }

    log.info("Query: " + queryInfo.getName());
  }

  private void loadChartMetric(ProcessType processType) {

    Metric metric = getMetricFromUI();

    executorService.submit(() -> {
      GUIHelper.addToJSplitPane(getChartGanttPanel(processType),
                                ProgressBarHelper.createProgressBar("Loading, please wait..."), JSplitPane.TOP, 250);

      try {
        loadChartByMetric(metric, processType);
      } catch (Exception exception) {
        log.catching(exception);
        throw new RuntimeException(exception);
      }
    });
  }

  protected Metric getMetricFromUI() {
    int metricId = GUIHelper.getIdByColumnName(jxTableCaseMetrics.getJxTable(),
                                               jxTableCaseMetrics.getDefaultTableModel(),
                                               jxTableCaseMetrics.getJxTable().getSelectionModel(),
                                               MetricsColumnNames.ID.getColName());

    log.info("Metric id: " + metricId);

    return queryInfo.getMetricList()
        .stream().filter(f -> f.getId() == metricId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found metric by id: " + metricId));
  }

  private void loadChartColumn(ProcessType processType) {
    CProfile cProfile = getCProfileFromUI();

    executorService.submit(() -> {
      GUIHelper.addToJSplitPane(getChartGanttPanel(processType),
                                ProgressBarHelper.createProgressBar("Loading, please wait..."), JSplitPane.TOP, 250);

      try {
        Metric metric = getMetricByCProfile(cProfile);
        setMetricFunctionAndChartType(metric);
        loadChartByMetric(metric, processType);
      } catch (Exception exception) {
        log.catching(exception);
        throw new RuntimeException(exception);
      }
    });
  }

  protected CProfile getCProfileFromUI() {
    int cProfileId = GUIHelper.getIdByColumnName(jxTableCaseColumns.getJxTable(),
                                                 jxTableCaseColumns.getDefaultTableModel(),
                                                 jxTableCaseColumns.getJxTable().getSelectionModel(),
                                                 ProfileColumnNames.ID.getColName());

    log.info("Column profile id: " + cProfileId);

    return tableInfo.getCProfiles()
        .stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found CProfile: " + cProfileId));
  }

  protected Metric getMetricByCProfile(CProfile cProfile) {
    Metric metric = new Metric();
    metric.setXAxis(tableInfo.getCProfiles().stream().filter(f -> f.getCsType().isTimeStamp()).findAny().orElseThrow());
    metric.setYAxis(cProfile);
    metric.setGroup(cProfile);

    if (MetricFunction.NONE.equals(metricFunctionOnEdit)) {
      setMetricFunctionAndChartNotEdit(cProfile, metric);
    } else {
      setMetricFunctionAndChartType(metric);
    }

    return metric;
  }

  private void setMetricFunctionAndChartType(Metric metric) {
    switch (metricFunctionOnEdit) {
      case COUNT -> {
        metric.setMetricFunction(metricFunctionOnEdit);
        metric.setChartType(ChartType.STACKED);
      }
      case SUM, AVG -> {
        metric.setMetricFunction(metricFunctionOnEdit);
        metric.setChartType(ChartType.LINEAR);
      }
    }
  }

  private void setMetricFunctionAndChartNotEdit(CProfile cProfile,
                                                Metric metric) {
    if (CType.STRING.equals(cProfile.getCsType().getCType())) {
      metric.setMetricFunction(MetricFunction.COUNT);
      metric.setChartType(ChartType.STACKED);
    } else {
      if (Arrays.stream(TimestampType.values()).anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        metric.setMetricFunction(MetricFunction.COUNT);
        metric.setChartType(ChartType.STACKED);
      } else {
        metric.setMetricFunction(MetricFunction.AVG);
        metric.setChartType(ChartType.LINEAR);
      }
    }
  }

  private void loadChartByMetric(Metric metric,
                                 ProcessType processType) {
    boolean isStackedYAxisSameCount = metric.isStackedYAxisSameCount();
    boolean isLinearYAxisSameSum = metric.isLinearYAxisSameSum();
    boolean isLinearYAxisAvg = metric.isLinearYAxisAvg();

    if (isStackedYAxisSameCount || isLinearYAxisSameSum || isLinearYAxisAvg) {
      addPanelToVisualize(metric, processType);
    }
  }

  private void addPanelToVisualize(Metric metric,
                                   ProcessType processType) {
    StackChartPanel stackChartPanel = getStackChartPanel(metric, processType);
    DetailPanel detailPanel = getDetailPanel(metric, stackChartPanel.getSeriesColorMap(), stackChartPanel, processType, stackChartPanel.getSeriesType());

    GUIHelper.addToJSplitPane(getChartGanttPanel(processType), stackChartPanel, JSplitPane.TOP, 250);
    GUIHelper.addToJSplitPane(getChartGanttPanel(processType), detailPanel, JSplitPane.BOTTOM, 250);

    if (HISTORY.equals(processType)) {
      CustomAction customAction = addPanelToAnalyze(metric, HISTORY_ANALYZE, stackChartPanel, stackChartPanel.getSeriesColorMap());

      if (stackChartPanel instanceof ServerClientHistorySCP serverClientHistorySCP) {
        serverClientHistorySCP.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
      }
    } else if (REAL_TIME.equals(processType)) {
      CustomAction customAction = addPanelToAnalyze(metric, REAL_TIME_ANALYZE, stackChartPanel, stackChartPanel.getSeriesColorMap());

      if (stackChartPanel instanceof ServerRealTimeSCP realtimeSCP) {
        realtimeSCP.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
      }

      if (stackChartPanel instanceof ClientRealTimeSCP realtimeSCP) {
        realtimeSCP.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
      }
    }
  }

  private CustomAction addPanelToAnalyze(Metric metric,
                                         ProcessType processType,
                                         StackChartPanel stackChartPanel,
                                         Map<String, Color> seriesColorMap) {
    if (HISTORY_ANALYZE.equals(processType)) {
      analyzeHistory.removeAll();

      AnalyzeHistoryPanel panel =
          new AnalyzeHistoryPanel(workspaceQueryComponent,
                                  queryInfo, tableInfo, chartInfo, profileTaskQueryKey, sourceConfig,
                                  metric, seriesColorMap, stackChartPanel.getSeriesType());

      if (SeriesType.CUSTOM.equals(stackChartPanel.getSeriesType())) {
        panel.setFilter(stackChartPanel.getFilter());
      }

      PGHelper.cellXYRemainder(analyzeHistory, panel, false);

      stackChartPanel.addChartListenerReleaseMouse(panel);

      analyzeHistory.revalidate();
      analyzeHistory.repaint();

      return panel;
    } else if (REAL_TIME_ANALYZE.equals(processType)) {
      analyzeRealTime.removeAll();

      AnalyzeRealtimePanel panel =
          new AnalyzeRealtimePanel(workspaceQueryComponent,
                                   queryInfo, tableInfo, chartInfo, profileTaskQueryKey, sourceConfig,
                                   metric, seriesColorMap, stackChartPanel.getSeriesType());

      if (SeriesType.CUSTOM.equals(stackChartPanel.getSeriesType())) {
        panel.setFilter(stackChartPanel.getFilter());
      }

      PGHelper.cellXYRemainder(analyzeRealTime, panel, false);

      stackChartPanel.addChartListenerReleaseMouse(panel);

      analyzeRealTime.revalidate();
      analyzeRealTime.repaint();

      return panel;
    }

    return null;
  }

  protected DetailPanel getDetailPanel(Metric metric,
                                       Map<String, Color> seriesColorMap,
                                       DetailChart dynamicChart,
                                       ProcessType processType,
                                       SeriesType seriesType) {
    DetailPanel detailPanel =
        new DetailPanel(workspaceQueryComponent, queryInfo, tableInfo, metric, seriesColorMap, processType, seriesType);

    dynamicChart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  protected StackChartPanel getStackChartPanel(Metric metric,
                                               ProcessType processType) {
    CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();

    StackChartPanel stackChartPanel = null;

    if (REAL_TIME.equals(processType)) {
      this.eventListener.clearListenerByKey(profileTaskQueryKey);

      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ClientRealTimeSCP(workspaceQueryComponent, chartDataset,
                                  profileTaskQueryKey,
                                  queryInfo, chartInfo, processType, metric);
      } else if (GatherDataMode.BY_SERVER_JDBC.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ServerRealTimeSCP(workspaceQueryComponent, chartDataset,
                                  profileTaskQueryKey,
                                  queryInfo, chartInfo, processType, metric);
      } else if (GatherDataMode.BY_CLIENT_HTTP.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ClientRealTimeSCP(workspaceQueryComponent, chartDataset,
                                  profileTaskQueryKey,
                                  queryInfo, chartInfo, processType, metric);
      }

      assert stackChartPanel != null;
      stackChartPanel.initialize();

      eventListener.addCollectStartStopListener(profileTaskQueryKey, stackChartPanel);
    } else if (ProcessType.HISTORY.equals(processType)) {
      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ServerClientHistorySCP(workspaceQueryComponent, chartDataset, profileTaskQueryKey,
                                       queryInfo, tableInfo, chartInfo, processType, metric);
      } else if (GatherDataMode.BY_SERVER_JDBC.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ServerClientHistorySCP(workspaceQueryComponent, chartDataset, profileTaskQueryKey,
                                       queryInfo, tableInfo, chartInfo, processType, metric);
      } else if (GatherDataMode.BY_CLIENT_HTTP.equals(queryInfo.getGatherDataMode())) {
        stackChartPanel =
            new ServerClientHistorySCP(workspaceQueryComponent, chartDataset, profileTaskQueryKey,
                                       queryInfo, tableInfo, chartInfo, processType, metric);
      }

      assert stackChartPanel != null;
      stackChartPanel.initialize();
    }

    return stackChartPanel;
  }

  private void checkTimestampColumn() {
    if (Objects.isNull(tableInfo.getCProfiles())) {
      throw new NotFoundException("Metadata not found, need to reload query metadata in configuration");
    }

    tableInfo.getCProfiles().stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow(
            () -> new NotFoundException("Not found column timestamp for table: " + tableInfo.getTableName()));
  }

  private JSplitPane getChartGanttPanel(ProcessType processType) {
    if (REAL_TIME.equals(processType)) {
      return visualizeRealTime;
    } else if (HISTORY.equals(processType)) {
      return visualizeHistory;
    } else if (REAL_TIME_ANALYZE.equals(processType)) {
      throw new RuntimeException("Not supported for " + processType);
    } else if (HISTORY_ANALYZE.equals(processType)) {
      throw new RuntimeException("Not supported for " + processType);
    } else {
      return new JSplitPane();
    }
  }

  private class RadioListenerColumn implements ActionListener {

    public RadioListenerColumn() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();
      RadioButtonActionExecutor.execute(button, metricFunction -> metricFunctionOnEdit = metricFunction);
    }
  }
}
