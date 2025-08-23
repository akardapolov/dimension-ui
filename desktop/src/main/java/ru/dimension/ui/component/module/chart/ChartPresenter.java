package ru.dimension.ui.component.module.chart;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.component.chart.realtime.RealtimeSCP;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.LogHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class ChartPresenter implements HelperChart, MessageAction {
  private final MessageBroker.Component component;
  private final ChartModel model;
  private final ChartView view;
  private final Metric realTimeMetric;
  private final Metric historyMetric;

  @Getter
  private boolean isReadyRealTimeUpdate = false;

  @Getter
  private SCP realTimeChart;
  private DetailDashboardPanel realTimeDetail;

  private SCP historyChart;
  private DetailDashboardPanel historyDetail;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartPresenter(MessageBroker.Component component,
                        ChartModel model,
                        ChartView view) {
    this.component = component;
    this.model = model;
    this.view = view;
    this.realTimeMetric = model.getMetric().copy();
    this.historyMetric = model.getMetric().copy();

    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getRealTimeMetricFunctionPanel().setRunAction(this::handleRealtimeMetricFunctionChange);
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);

    view.getRealTimeLegendPanel().setStateChangeConsumer(showLegend ->
                                                            handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryMetricFunctionPanel().setRunAction(this::handleHistoryMetricFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);

    view.getHistoryLegendPanel().setStateChangeConsumer(showLegend ->
                                                           handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());

    initializeFilterPanels();
  }

  public void initializeCharts() {
    createRealTimeChart();
    createHistoryChart();
  }

  private void createRealTimeChart() {
    SwingTaskRunner.runWithProgress(
        view.getRealTimeChartPanel(),
        executor,
        () -> {
          isReadyRealTimeUpdate = false;

          realTimeChart = createChart(AnalyzeType.REAL_TIME, null);
          realTimeChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          SeriesType seriesTypeChart = realTimeChart.getSeriesType();

          if (SeriesType.COMMON.equals(seriesTypeChart)) {
            realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart, null, SeriesType.COMMON, null);
          } else {
            RealtimeSCP realtimeSCP = (RealtimeSCP) realTimeChart;
            realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart, realTimeChart.getSeriesColorMap(), SeriesType.CUSTOM, realtimeSCP.getFilter());
          }

          ChartKey chartKey = model.getChartKey();
          long end = model.getSqlQueryState().getLastTimestamp(chartKey.getProfileTaskQueryKey());
          if (end == 0L) {
            end = DateHelper.getNowMilli(ZoneId.systemDefault());
          }
          long begin = end - getRangeRealTime(model.getChartInfo());

          view.getRealTimeFilterPanel().setDataSource(model.getDStore(), realTimeMetric, begin, end);

          view.getRealTimeFilterPanel().clearFilterPanel();
          view.getRealTimeFilterPanel().setSeriesColorMap(realTimeChart.getSeriesColorMap());
          view.getRealTimeFilterPanel().getMetric().setMetricFunction(realTimeChart.getConfig().getMetric().getMetricFunction());
          return () -> {
            view.getRealTimeChartPanel().removeAll();
            view.getRealTimeDetailPanel().removeAll();

            view.getRealTimeChartPanel().add(realTimeChart, BorderLayout.CENTER);
            view.getRealTimeDetailPanel().add(realTimeDetail, BorderLayout.CENTER);

            isReadyRealTimeUpdate = true;

            boolean isCustom = realTimeChart.getSeriesType() == SeriesType.CUSTOM;
            view.getRealTimeFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error creating real-time chart", e);
        },
        () -> createProgressBar("Creating real-time chart..."),
        () -> setDetailState(PanelTabType.REALTIME, UIState.INSTANCE.getShowDetailAll(component.name()))
    );
  }

  private void createHistoryChart() {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          historyChart = createChart(AnalyzeType.HISTORY, null);
          historyChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          SeriesType seriesTypeChart = historyChart.getSeriesType();

          if (SeriesType.COMMON.equals(seriesTypeChart)) {
            historyDetail = getDetail(AnalyzeType.HISTORY, historyChart, null, SeriesType.COMMON, null);
          } else {
            HistorySCP historySCP = (HistorySCP) historyChart;
            historyDetail = getDetail(AnalyzeType.HISTORY, historyChart, historyChart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getTopMapSelected());
          }

          ChartRange chartRange = getChartRange(historyChart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(model.getDStore(), historyMetric, chartRange.getBegin(), chartRange.getEnd());

          view.getHistoryFilterPanel().clearFilterPanel();
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setMetricFunction(historyChart.getConfig().getMetric().getMetricFunction());
          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(component.name()))
    );
  }

  private SCP createChart(AnalyzeType analyzeType, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig(analyzeType);
    ProfileTaskQueryKey key = model.getKey();
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();
    QueryInfo queryInfo = model.getQueryInfo();

    if (analyzeType == AnalyzeType.REAL_TIME) {
      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        return new ClientRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
      } else {
        return new ServerRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
      }
    } else if (analyzeType == AnalyzeType.HISTORY) {
      ChartRange chartRange = getChartRange(config.getChartInfo());

      config.getChartInfo().setCustomBegin(chartRange.getBegin());
      config.getChartInfo().setCustomEnd(chartRange.getEnd());

      return new HistorySCP(dStore, config, key, topMapSelected);
    } else {
      throw new RuntimeException("Not supported");
    }
  }

  protected DetailDashboardPanel getDetail(AnalyzeType analyzeType,
                                           SCP chart,
                                           Map<String, Color> initialSeriesColorMap,
                                           SeriesType seriesType,
                                           Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ProcessType processType = null;

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      processType = ProcessType.REAL_TIME;
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      processType = ProcessType.HISTORY;
    }

    Map<String, Color> seriesColorMapToUse = initialSeriesColorMap != null ?
        initialSeriesColorMap :
        chart.getSeriesColorMap();

    if (SeriesType.CUSTOM.equals(seriesType)) {
      seriesColorMapToUse = chart.getSeriesColorMap();
    }

    Metric chartMetric = chart.getConfig().getMetric();

    DetailDashboardPanel detailPanel =
        new DetailDashboardPanel(model.getDStore(),
                                 model.getQueryInfo(),
                                 model.getTableInfo(),
                                 chartMetric,
                                 seriesColorMapToUse,
                                 processType,
                                 seriesType,
                                 topMapSelected);

    CustomAction customAction = new CustomAction() {

      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {}

      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                        Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        topMapSelected.values().forEach(set -> set.forEach(value -> newSeriesColorMap.put(value, seriesColorMap.get(value))));

        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
        detailPanel.setSeriesType(SeriesType.CUSTOM);
      }

      @Override
      public void setBeginEnd(long begin,
                              long end) {}
    };

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));

    chart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  private ChartConfig buildChartConfig(AnalyzeType analyzeType) {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
    Metric baseMetric = analyzeType == AnalyzeType.REAL_TIME ? realTimeMetric : historyMetric;
    Metric metricCopy = baseMetric.copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      MetricFunction metricFunction = UIState.INSTANCE.getRealtimeMetricFunction(chartKey);
      if (metricFunction != null) {
        metricCopy.setMetricFunction(metricFunction);
        metricCopy.setChartType(MetricFunction.COUNT.equals(metricFunction) ? ChartType.STACKED : ChartType.LINEAR);
      }

      RangeRealTime rangeRealTime = UIState.INSTANCE.getRealTimeRange(chartKey);
      chartInfoCopy.setRangeRealtime(Objects.requireNonNullElse(rangeRealTime, RangeRealTime.TEN_MIN));

    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(chartKey);
      if (metricFunction != null) {
        metricCopy.setMetricFunction(metricFunction);
        metricCopy.setChartType(MetricFunction.COUNT.equals(metricFunction) ? ChartType.STACKED : ChartType.LINEAR);
      }

      RangeHistory rangeHistory = UIState.INSTANCE.getHistoryRange(chartKey);
      chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(rangeHistory, RangeHistory.DAY));

      if (chartInfoCopy.getRangeHistory() == RangeHistory.CUSTOM) {
        ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(chartKey);
        if (customRange != null) {
          chartInfoCopy.setCustomBegin(customRange.getBegin());
          chartInfoCopy.setCustomEnd(customRange.getEnd());

          UIState.INSTANCE.putHistoryCustomRange(chartKey, customRange);
        } else {
          ChartRange chartRange = getChartRangeFromHistoryRangePanel();

          chartInfoCopy.setCustomBegin(chartRange.getBegin());
          chartInfoCopy.setCustomEnd(chartRange.getEnd());

          UIState.INSTANCE.putHistoryCustomRange(chartKey, chartRange);
        }
      }
    }

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      RangeRealTime localRange = UIState.INSTANCE.getRealTimeRange(chartKey);
      RangeRealTime globalRange = UIState.INSTANCE.getRealTimeRangeAll(component.name());
      chartInfoCopy.setRangeRealtime(Objects.requireNonNullElse(localRange,
                                                                Objects.requireNonNullElse(globalRange, RangeRealTime.TEN_MIN)));
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      RangeHistory localRange = UIState.INSTANCE.getHistoryRange(chartKey);
      RangeHistory globalRange = UIState.INSTANCE.getHistoryRangeAll(component.name());
      chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(localRange,
                                                               Objects.requireNonNullElse(globalRange, RangeHistory.DAY)));
    }

    config.setChartKey(chartKey);

    config.setTitle("");
    config.setXAxisLabel(model.getMetric().getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metricCopy);
    config.setChartInfo(chartInfoCopy);
    config.setQueryInfo(model.getQueryInfo());

    return config;
  }

  private void initializeFromState() {
    ChartKey chartKey = model.getChartKey();

    MetricFunction realTimeMetricFunction = UIState.INSTANCE.getRealtimeMetricFunction(chartKey);
    if (realTimeMetricFunction != null) {
      realTimeMetric.setMetricFunction(realTimeMetricFunction);
      realTimeMetric.setChartType(MetricFunction.COUNT.equals(realTimeMetricFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }
    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getRealTimeMetricFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getRealTimeMetricFunctionPanel().setEnabled(true, true, true);
    }
    view.getRealTimeMetricFunctionPanel().setSelected(realTimeMetric.getMetricFunction());

    MetricFunction historyMetricFunction = UIState.INSTANCE.getHistoryMetricFunction(chartKey);
    if (historyMetricFunction != null) {
      historyMetric.setMetricFunction(historyMetricFunction);
      historyMetric.setChartType(MetricFunction.COUNT.equals(historyMetricFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }
    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryMetricFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryMetricFunctionPanel().setEnabled(true, true, true);
    }
    view.getHistoryMetricFunctionPanel().setSelected(historyMetric.getMetricFunction());

    RangeRealTime localRealTimeRange = UIState.INSTANCE.getRealTimeRange(chartKey);
    RangeRealTime globalRealTimeRange = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    RangeRealTime effectiveRealTimeRange = localRealTimeRange != null ? localRealTimeRange : globalRealTimeRange;

    if (effectiveRealTimeRange != null) {
      view.getRealTimeRangePanel().setSelectedRange(effectiveRealTimeRange);
    }

    RangeHistory localHistoryRange = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory globalHistoryRange = UIState.INSTANCE.getHistoryRangeAll(component.name());
    RangeHistory effectiveHistoryRange = localHistoryRange != null ? localHistoryRange : globalHistoryRange;

    if (effectiveHistoryRange != null) {
      view.getHistoryRangePanel().setSelectedRange(effectiveHistoryRange);
    }

    if (effectiveHistoryRange == RangeHistory.CUSTOM) {
      ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(chartKey);
      if (customRange != null) {
        view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
        view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
      }
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey);
    if (showLegend != null) {
      view.getRealTimeLegendPanel().setSelected(showLegend);
      view.getHistoryLegendPanel().setSelected(showLegend);
    }
  }

  private void handleRealtimeMetricFunctionChange(String action, MetricFunction function) {
    realTimeMetric.setMetricFunction(function);
    realTimeMetric.setChartType(MetricFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putRealtimeMetricFunction(model.getChartKey(), function);

    view.getRealTimeFilterPanel().clearFilterPanel();
    updateRealTimeChart();
  }

  private void handleHistoryMetricFunctionChange(String action, MetricFunction function) {
    historyMetric.setMetricFunction(function);
    historyMetric.setChartType(MetricFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putHistoryMetricFunction(model.getChartKey(), function);

    view.getHistoryFilterPanel().clearFilterPanel();
    updateHistoryChart();
  }

  public void handleRealTimeRangeChange(String action,
                                        RangeRealTime range) {
    UIState.INSTANCE.putRealTimeRange(model.getChartKey(), range);

    model.getChartInfo().setRangeRealtime(range);

    setActiveTab(PanelTabType.REALTIME);

    updateRealTimeChart();
  }

  public void handleHistoryRangeChange(String action,
                                       RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);

    view.getHistoryRangePanel().setSelectedRange(range);

    setActiveTab(PanelTabType.HISTORY);
    updateHistoryChart();
  }

  public void setActiveTab(PanelTabType panelTabType) {
    view.getTabbedPane().setSelectedIndex(panelTabType.ordinal());
  }

  public void updateRealTimeRange(RangeRealTime range) {
    view.getRealTimeRangePanel().setSelectedRange(range);
    handleRealTimeRangeChange("configChange", range);
  }

  public void updateHistoryRange(RangeHistory range) {
    view.getHistoryRangePanel().setSelectedRange(range);
    handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    UIState.INSTANCE.putHistoryCustomRange(model.getChartKey(), range);
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), RangeHistory.CUSTOM);
    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    view.getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(range.getBegin()));
    view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(range.getEnd()));

    updateHistoryChart();
  }

  public void setDetailState(DetailState detailState) {
    boolean showDetail = detailState == DetailState.SHOW;
    log.info("Setting detail visibility to: {} for chart {}", showDetail, model.getChartKey());

    view.setRealTimeDetailVisible(showDetail);
    view.setHistoryDetailVisible(showDetail);

    realTimeChart.clearSelectionRegion();
    historyChart.clearSelectionRegion();
  }

  public void setDetailState(PanelTabType panelTabType, DetailState detailState) {
    boolean showDetail = detailState == DetailState.SHOW;
    log.info("Setting detail visibility to: {} for chart {}", showDetail, model.getChartKey());

    switch (panelTabType) {
      case REALTIME -> {
        view.setRealTimeDetailVisible(showDetail);
        realTimeChart.clearSelectionRegion();
      }
      case HISTORY -> {
        view.setHistoryDetailVisible(showDetail);
        historyChart.clearSelectionRegion();
      }
      default -> log.warn("Unknown panel tab type: {}", panelTabType);
    }
  }
  public void handleLegendChangeAll(Boolean showLegend) {
    Boolean showLegendAll = UIState.INSTANCE.getShowLegendAll(component.name());
    boolean effectiveVisibility = (showLegendAll != null && showLegendAll) ||
        (showLegend != null ? showLegend : false);

    updateLegendVisibility(effectiveVisibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), effectiveVisibility);
  }

  private void handleLegendChange(Boolean showLegend) {
    boolean visibility = showLegend != null ? showLegend : true;

    updateLegendVisibility(visibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), visibility);
  }

  private void applyCustomRange() {
    HistoryRangePanel rangePanel = view.getHistoryRangePanel();

    rangePanel.getButtonGroup().clearSelection();
    rangePanel.getCustom().setSelected(true);
    rangePanel.colorButton(RangeHistory.CUSTOM);

    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();
    ChartRange chartRange = new ChartRange(from.getTime(), to.getTime());

    UIState.INSTANCE.putHistoryCustomRange(model.getChartKey(), chartRange);

    handleHistoryRangeChange("rangeChanged", RangeHistory.CUSTOM);
  }

  private ChartRange getChartRangeFromHistoryRangePanel() {
    HistoryRangePanel rangePanel = view.getHistoryRangePanel();

    rangePanel.getButtonGroup().clearSelection();
    rangePanel.getCustom().setSelected(true);
    rangePanel.colorButton(RangeHistory.CUSTOM);

    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();

    return new ChartRange(from.getTime(), to.getTime());
  }

  private void updateLegendVisibility(boolean visibility) {
    if (realTimeChart != null) {
      view.getRealTimeLegendPanel().setSelected(visibility);
      realTimeChart.getjFreeChart().getLegend().setVisible(visibility);
      realTimeChart.repaint();
    }
    if (historyChart != null) {
      view.getHistoryLegendPanel().setSelected(visibility);
      historyChart.getjFreeChart().getLegend().setVisible(visibility);
      historyChart.repaint();
    }
  }

  public void initializeFilterPanels() {
    view.getRealTimeFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.REALTIME);
    view.getHistoryFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.HISTORY);
  }

  @Override
  public void receive(Message message) {
    log.info("Message received >>> " + message.destination() + " with action >>> " + message.action());

    PanelTabType panelTabType = PanelTabType.valueOf(message.destination().panel().name());

    Map<CProfile, LinkedHashSet<String>> topMapSelected = message.parameters().get("topMapSelected");

    LogHelper.logMapSelected(topMapSelected);

    Map<String, Color> seriesColorMap = message.parameters().get("seriesColorMap");

    switch (message.action()) {
      case ADD_CHART_FILTER -> handleFilterChange(panelTabType, topMapSelected, seriesColorMap);
      case REMOVE_CHART_FILTER -> handleFilterChange(panelTabType, null, seriesColorMap);
    }
  }

  private Map<String, Color> getFilterSeriesColorMap(Metric metric,
                                                     Map<String, Color> seriesColorMap,
                                                     Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    if (topMapSelected == null
        || topMapSelected.isEmpty()
        || topMapSelected.values().stream().allMatch(LinkedHashSet::isEmpty)
        || !topMapSelected.containsKey(metric.getYAxis())) {
      return seriesColorMap;
    }

    Map<String, Color> filteredMap = new HashMap<>();
    for (String key : topMapSelected.get(metric.getYAxis())) {
      if (seriesColorMap.containsKey(key)) {
        filteredMap.put(key, seriesColorMap.get(key));
      }
    }
    return filteredMap;
  }

  private void handleFilterChange(PanelTabType panelTabType,
                                  Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                  Map<String, Color> seriesColorMap) {
    Map<String, Color> preservedColorMap = new HashMap<>(seriesColorMap);

    if (panelTabType == PanelTabType.REALTIME) {
      updateRealTimeChart(preservedColorMap, topMapSelected);
    } else if (panelTabType == PanelTabType.HISTORY) {
      updateHistoryChart(preservedColorMap, topMapSelected);
    }

    if (panelTabType == PanelTabType.REALTIME && realTimeDetail != null) {
      realTimeDetail.updateSeriesColor(topMapSelected, preservedColorMap);
    } else if (panelTabType == PanelTabType.HISTORY && historyDetail != null) {
      historyDetail.updateSeriesColor(topMapSelected, preservedColorMap);
    }
  }

  private void updateRealTimeChart() {
    updateRealTimeChart(null, null);
  }

  private void updateRealTimeChart(Map<String, Color> seriesColorMap, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    SwingTaskRunner.runWithProgress(
        view.getRealTimeChartPanel(),
        executor,
        () -> {
          isReadyRealTimeUpdate = false;

          view.getRealTimeChartPanel().removeAll();
          view.getRealTimeDetailPanel().removeAll();

          view.getRealTimeConfigChartDetail().revalidate();
          view.getRealTimeConfigChartDetail().repaint();

          view.getRealTimeChartDetailSplitPane().revalidate();
          view.getRealTimeChartDetailSplitPane().repaint();

          realTimeChart = null;
          realTimeDetail = null;

          realTimeChart = createChart(AnalyzeType.REAL_TIME, topMapSelected);

          if (seriesColorMap != null) {
            realTimeChart.loadSeriesColor(realTimeMetric, seriesColorMap);
          }
          realTimeChart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(realTimeMetric, seriesColorMap, topMapSelected);
            realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart, filterSeriesColorMap, SeriesType.CUSTOM, topMapSelected);
          } else {
            SeriesType seriesTypeChart = realTimeChart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart, null, SeriesType.COMMON, null);
            } else {
              RealtimeSCP realtimeSCP = (RealtimeSCP) realTimeChart;
              realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart, realTimeChart.getSeriesColorMap(), SeriesType.CUSTOM, realtimeSCP.getFilter());
            }
          }

          ChartKey chartKey = model.getChartKey();
          long end = model.getSqlQueryState().getLastTimestamp(chartKey.getProfileTaskQueryKey());
          if (end == 0L) {
            end = DateHelper.getNowMilli(ZoneId.systemDefault());
          }
          long begin = end - getRangeRealTime(model.getChartInfo());
          view.getRealTimeFilterPanel().setDataSource(model.getDStore(), realTimeMetric, begin, end);

          if (seriesColorMap == null) {
            view.getRealTimeFilterPanel().clearFilterPanel();
          }
          view.getRealTimeFilterPanel().setSeriesColorMap(realTimeChart.getSeriesColorMap());
          view.getRealTimeFilterPanel().getMetric().setMetricFunction(realTimeChart.getConfig().getMetric().getMetricFunction());
          return () -> {
            view.getRealTimeChartPanel().add(realTimeChart, BorderLayout.CENTER);
            view.getRealTimeDetailPanel().add(realTimeDetail, BorderLayout.CENTER);

            isReadyRealTimeUpdate = true;

            boolean isCustom = realTimeChart.getSeriesType() == SeriesType.CUSTOM;
            view.getRealTimeFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error updating real-time chart", e);
        },
        () -> createProgressBar("Updating real-time chart..."),
        () -> setDetailState(PanelTabType.REALTIME, UIState.INSTANCE.getShowDetailAll(component.name()))
    );
  }

  private void updateHistoryChart() {
    updateHistoryChart(null, null);
  }

  private void updateHistoryChart(Map<String, Color> seriesColorMap, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          view.getHistoryConfigChartDetail().revalidate();
          view.getHistoryConfigChartDetail().repaint();

          view.getHistoryChartDetailSplitPane().revalidate();
          view.getHistoryChartDetailSplitPane().repaint();

          historyChart = null;
          historyDetail = null;

          historyChart = createChart(AnalyzeType.HISTORY, topMapSelected);

          if (seriesColorMap != null) {
            historyChart.loadSeriesColor(historyMetric, seriesColorMap);
          }
          historyChart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(historyMetric, seriesColorMap, topMapSelected);
            historyDetail = getDetail(AnalyzeType.HISTORY, historyChart, filterSeriesColorMap, SeriesType.CUSTOM, topMapSelected);
          } else {
            SeriesType seriesTypeChart = historyChart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              historyDetail = getDetail(AnalyzeType.HISTORY, historyChart, null, SeriesType.COMMON, null);
            } else {
              HistorySCP historySCP = (HistorySCP) historyChart;
              historyDetail = getDetail(AnalyzeType.HISTORY, historyChart, historyChart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getTopMapSelected());
            }
          }

          ChartRange chartRange = getChartRange(historyChart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(model.getDStore(), historyMetric, chartRange.getBegin(), chartRange.getEnd());

          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setMetricFunction(historyChart.getConfig().getMetric().getMetricFunction());
          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(component.name()))
    );
  }
}