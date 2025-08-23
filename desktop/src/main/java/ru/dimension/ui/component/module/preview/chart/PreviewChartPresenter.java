package ru.dimension.ui.component.module.preview.chart;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.time.ZoneId;
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
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.LogHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PreviewChartPresenter implements HelperChart, MessageAction {
  private final MessageBroker.Component component;
  private final PreviewChartModel model;
  private final PreviewChartView view;
  private final Metric realTimeMetric;

  @Getter
  private boolean isReadyRealTimeUpdate = false;

  @Getter
  private SCP realTimeChart;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PreviewChartPresenter(MessageBroker.Component component,
                               PreviewChartModel model,
                               PreviewChartView view) {
    this.component = component;
    this.model = model;
    this.view = view;
    this.realTimeMetric = model.getMetric().copy();

    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getRealTimeMetricFunctionPanel().setRunAction(this::handleRealtimeMetricFunctionChange);
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);

    view.getRealTimeLegendPanel().setStateChangeConsumer(showLegend ->
                                                            handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    createRealTimeChart();
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
            view.getRealTimeChartPanel().add(realTimeChart, BorderLayout.CENTER);
            isReadyRealTimeUpdate = true;
            boolean isCustom = realTimeChart.getSeriesType() == SeriesType.CUSTOM;
            view.getRealTimeFilterPanel().setEnabled(false);
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error creating real-time chart", e);
        },
        () -> createProgressBar("Creating real-time chart...")
    );
  }

  private SCP createChart(AnalyzeType analyzeType, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig(analyzeType);
    ProfileTaskQueryKey key = model.getKey();
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();
    QueryInfo queryInfo = model.getQueryInfo();

    if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
      return new ClientRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
    } else {
      return new ServerRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
    }
  }

  private ChartConfig buildChartConfig(AnalyzeType analyzeType) {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
    Metric baseMetric = realTimeMetric;
    Metric metricCopy = baseMetric.copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();

    MetricFunction metricFunction = UIState.INSTANCE.getRealtimeMetricFunction(chartKey);
    if (metricFunction != null) {
      metricCopy.setMetricFunction(metricFunction);
      metricCopy.setChartType(MetricFunction.COUNT.equals(metricFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    RangeRealTime rangeRealTime = UIState.INSTANCE.getRealTimeRange(chartKey);
    chartInfoCopy.setRangeRealtime(Objects.requireNonNullElse(rangeRealTime, RangeRealTime.TEN_MIN));

    RangeRealTime localRange = UIState.INSTANCE.getRealTimeRange(chartKey);
    RangeRealTime globalRange = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    chartInfoCopy.setRangeRealtime(Objects.requireNonNullElse(localRange,
                                                              Objects.requireNonNullElse(globalRange, RangeRealTime.TEN_MIN)));

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
    view.getRealTimeMetricFunctionPanel().setSelected(realTimeMetric.getMetricFunction());

    RangeRealTime localRealTimeRange = UIState.INSTANCE.getRealTimeRange(chartKey);
    RangeRealTime globalRealTimeRange = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    RangeRealTime effectiveRealTimeRange = localRealTimeRange != null ? localRealTimeRange : globalRealTimeRange;

    if (effectiveRealTimeRange != null) {
      view.getRealTimeRangePanel().setSelectedRange(effectiveRealTimeRange);
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey);
    if (showLegend != null) {
      view.getRealTimeLegendPanel().setSelected(showLegend);
    }
  }

  private void handleRealtimeMetricFunctionChange(String action, MetricFunction function) {
    realTimeMetric.setMetricFunction(function);
    realTimeMetric.setChartType(MetricFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putRealtimeMetricFunction(model.getChartKey(), function);

    view.getRealTimeFilterPanel().clearFilterPanel();
    updateRealTimeChart();
  }

  public void handleRealTimeRangeChangeUI(RangeRealTime range) {
    UIState.INSTANCE.putRealTimeRange(model.getChartKey(), range);
    model.getChartInfo().setRangeRealtime(range);
    view.getRealTimeRangePanel().setSelectedRange(range);
  }

  public void handleRealTimeRangeChange(String action, RangeRealTime range) {
    UIState.INSTANCE.putRealTimeRange(model.getChartKey(), range);
    model.getChartInfo().setRangeRealtime(range);
    view.getRealTimeRangePanel().setSelectedRange(range);
    updateRealTimeChart();
  }

  public void handleLegendChange(Boolean showLegend) {
    boolean visibility = showLegend != null ? showLegend : true;
    updateLegendVisibility(visibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), visibility);
  }

  private void updateLegendVisibility(boolean visibility) {
    if (realTimeChart != null) {
      view.getRealTimeLegendPanel().setSelected(visibility);
      realTimeChart.getjFreeChart().getLegend().setVisible(visibility);
      realTimeChart.repaint();
    }
  }

  public void initializeFilterPanels() {
    view.getRealTimeFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.REALTIME);
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
          view.getRealTimeChartPanel().revalidate();
          view.getRealTimeChartPanel().repaint();

          realTimeChart = null;

          realTimeChart = createChart(AnalyzeType.REAL_TIME, topMapSelected);

          if (seriesColorMap != null) {
            realTimeChart.loadSeriesColor(realTimeMetric, seriesColorMap);
          }
          realTimeChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

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
            isReadyRealTimeUpdate = true;
            boolean isCustom = realTimeChart.getSeriesType() == SeriesType.CUSTOM;
            view.getRealTimeFilterPanel().setEnabled(false);
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error updating real-time chart", e);
        },
        () -> createProgressBar("Updating real-time chart...")
    );
  }

  public void handleChartConfigState(ChartConfigState detailState) {
    boolean showDetail = detailState == ChartConfigState.SHOW;
    log.info("Setting config chart visibility to: {} for chart {}", showDetail, model.getChartKey());

    view.setChartConfigState(showDetail);

    if (realTimeChart != null) {
      realTimeChart.clearSelectionRegion();
    }
  }
}