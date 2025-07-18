package ru.dimension.ui.component.module.chart;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
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
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.analyze.CustomAction;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.chart.SCP;
import ru.dimension.ui.view.analyze.chart.history.HistorySCP;
import ru.dimension.ui.view.analyze.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.view.analyze.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.model.DetailState;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class ChartPresenter implements HelperChart {
  private final ChartModel model;
  private final ChartView view;

  @Getter
  private boolean isReadyRealTimeUpdate = false;

  @Getter
  private SCP realTimeChart;
  private DetailDashboardPanel realTimeDetail;

  private SCP historyChart;
  private DetailDashboardPanel historyDetail;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartPresenter(ChartModel model, ChartView view) {
    this.model = model;
    this.view = view;

    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getRealTimeMetricFunctionPanel().setRunAction(this::handleRealtimeMetricFunctionChange);
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);

    view.getRealTimeLegendPanel().setVisibilityConsumer(showLegend ->
                                                            handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryMetricFunctionPanel().setRunAction(this::handleHistoryMetricFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);

    view.getHistoryLegendPanel().setVisibilityConsumer(showLegend ->
                                                           handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());
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

          realTimeChart = createChart(AnalyzeType.REAL_TIME);
          realTimeChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart);

          return () -> {
            view.getRealTimeChartPanel().removeAll();
            view.getRealTimeDetailPanel().removeAll();

            view.getRealTimeChartPanel().add(realTimeChart, BorderLayout.CENTER);
            view.getRealTimeDetailPanel().add(realTimeDetail, BorderLayout.CENTER);

            isReadyRealTimeUpdate = true;
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error creating real-time chart", e);
        },
        () -> createProgressBar("Creating real-time chart..."),
        () -> setDetailState(PanelTabType.REALTIME, UIState.INSTANCE.getShowDetailAll(Component.DASHBOARD.name()))
    );
  }

  private void createHistoryChart() {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          historyChart = createChart(AnalyzeType.HISTORY);
          historyChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          historyDetail = getDetail(AnalyzeType.HISTORY, historyChart);

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(Component.DASHBOARD.name()))
    );
  }

  private SCP createChart(AnalyzeType analyzeType) {
    ChartConfig config = buildChartConfig(analyzeType);
    ProfileTaskQueryKey key = model.getKey();
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();
    QueryInfo queryInfo = model.getQueryInfo();

    if (analyzeType == AnalyzeType.REAL_TIME) {
      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        return new ClientRealtimeSCP(sqlQueryState, dStore, config, key, null);
      } else {
        return new ServerRealtimeSCP(sqlQueryState, dStore, config, key, null);
      }
    } else if (analyzeType == AnalyzeType.HISTORY) {
      ChartRange chartRange = getChartRange(config.getChartInfo());

      config.getChartInfo().setCustomBegin(chartRange.getBegin());
      config.getChartInfo().setCustomEnd(chartRange.getEnd());

      return new HistorySCP(dStore, config, key, null);
    } else {
      throw new RuntimeException("Not supported");
    }
  }

  protected DetailDashboardPanel getDetail(AnalyzeType analyzeType, SCP chart) {
    ProcessType processType = null;

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      processType = ProcessType.REAL_TIME;
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      processType = ProcessType.HISTORY;
    }

    Metric chartMetric = chart.getConfig().getMetric();

    DetailDashboardPanel detailPanel =
        new DetailDashboardPanel(model.getDStore(),
                                 model.getQueryInfo(),
                                 model.getTableInfo(),
                                 chartMetric,
                                 chart.getSeriesColorMap(),
                                 processType,
                                 chart.getSeriesType());

    CustomAction customAction = new CustomAction() {

      @Override
      public void setCustomSeriesFilter(CProfile profile,
                                        List<String> series) {
      }

      @Override
      public void setCustomSeriesFilter(CProfile cProfileFilter,
                                        List<String> filter,
                                        Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        filter.forEach(key -> newSeriesColorMap.put(key, seriesColorMap.get(key)));

        detailPanel.updateSeriesColor(newSeriesColorMap);
      }

      @Override
      public void setBeginEnd(long begin,
                              long end) {

      }
    };

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));

    chart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  private ChartConfig buildChartConfig(AnalyzeType analyzeType) {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
    Metric metricCopy = model.getMetric().copy();
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
      RangeRealTime globalRange = UIState.INSTANCE.getRealTimeRangeAll(Component.DASHBOARD.name());
      chartInfoCopy.setRangeRealtime(Objects.requireNonNullElse(localRange,
                                                                Objects.requireNonNullElse(globalRange, RangeRealTime.TEN_MIN)));
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      RangeHistory localRange = UIState.INSTANCE.getHistoryRange(chartKey);
      RangeHistory globalRange = UIState.INSTANCE.getHistoryRangeAll(Component.DASHBOARD.name());
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

    // Real-time metric function
    MetricFunction realTimeMetricFunction = UIState.INSTANCE.getRealtimeMetricFunction(chartKey);
    if (realTimeMetricFunction == null) {
      if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
        view.getRealTimeMetricFunctionPanel().setEnabled(true, false, false);
      } else {
        view.getRealTimeMetricFunctionPanel().setEnabled(true, true, true);
      }

      view.getRealTimeMetricFunctionPanel().setSelected(model.getMetric().getMetricFunction());
    } else {
      view.getRealTimeMetricFunctionPanel().setSelected(realTimeMetricFunction);
    }

    // History metric function
    MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(chartKey);
    if (metricFunction == null) {
      if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
        view.getRealTimeMetricFunctionPanel().setEnabled(true, false, false);
      } else {
        view.getRealTimeMetricFunctionPanel().setEnabled(true, true, true);
      }

      view.getHistoryMetricFunctionPanel().setSelected(model.getMetric().getMetricFunction());
    } else {
      view.getHistoryMetricFunctionPanel().setSelected(metricFunction);
    }

    // Real-time Range (local state first, then global)
    RangeRealTime localRealTimeRange = UIState.INSTANCE.getRealTimeRange(chartKey);
    RangeRealTime globalRealTimeRange = UIState.INSTANCE.getRealTimeRangeAll(Component.DASHBOARD.name());
    RangeRealTime effectiveRealTimeRange = localRealTimeRange != null ? localRealTimeRange : globalRealTimeRange;

    if (effectiveRealTimeRange != null) {
      view.getRealTimeRangePanel().setSelectedRange(effectiveRealTimeRange);
    }

    // History Range (local state first, then global)
    RangeHistory localHistoryRange = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory globalHistoryRange = UIState.INSTANCE.getHistoryRangeAll(Component.DASHBOARD.name());
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

    // Legend Visibility
    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey);
    if (showLegend != null) {
      view.getRealTimeLegendPanel().setSelected(showLegend);
      view.getHistoryLegendPanel().setSelected(showLegend);
    }
  }

  private void handleRealtimeMetricFunctionChange(String action, MetricFunction function) {
    UIState.INSTANCE.putRealtimeMetricFunction(model.getChartKey(), function);
    updateRealTimeChart();
  }

  private void handleHistoryMetricFunctionChange(String action, MetricFunction function) {
    UIState.INSTANCE.putHistoryMetricFunction(model.getChartKey(), function);
    updateHistoryChart();
  }

  private void updateRealTimeChart() {
    SwingTaskRunner.runWithProgress(
        view.getRealTimeChartPanel(),
        executor,
        () -> {
          isReadyRealTimeUpdate = false;

          view.getRealTimeChartPanel().removeAll();
          view.getRealTimeDetailPanel().removeAll();

          realTimeChart = null;
          realTimeDetail = null;

          realTimeChart = createChart(AnalyzeType.REAL_TIME);
          realTimeChart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          realTimeDetail = getDetail(AnalyzeType.REAL_TIME, realTimeChart);

          return () -> {
            view.getRealTimeChartPanel().add(realTimeChart, BorderLayout.CENTER);
            view.getRealTimeDetailPanel().add(realTimeDetail, BorderLayout.CENTER);

            isReadyRealTimeUpdate = true;
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error updating real-time chart", e);
        },
        () -> createProgressBar("Updating real-time chart..."),
        () -> setDetailState(PanelTabType.REALTIME, UIState.INSTANCE.getShowDetailAll(Component.DASHBOARD.name()))
    );
  }

  private void updateHistoryChart() {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          historyChart = null;
          historyDetail = null;

          historyChart = createChart(AnalyzeType.HISTORY);
          historyChart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          historyDetail = getDetail(AnalyzeType.HISTORY, historyChart);

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(Component.DASHBOARD.name()))
    );
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
    Boolean showLegendAll = UIState.INSTANCE.getShowLegendAll(Component.DASHBOARD.name());
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
}