package ru.dimension.ui.component.module.chart.unit;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.module.chart.ChartModel;
import ru.dimension.ui.helper.FilterHelper;
import ru.dimension.ui.helper.LogHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class HistoryUnitPresenter implements HelperChart {

  private final MessageBroker.Component component;
  private final ChartModel model;
  private final HistoryUnitView view;
  private final ExecutorService executor;

  private final Metric metric;

  @Getter
  private SCP chart;
  private DetailDashboardPanel detail;

  public HistoryUnitPresenter(MessageBroker.Component component,
                              ChartModel model,
                              HistoryUnitView view,
                              ExecutorService executor) {
    this.component = component;
    this.model = model;
    this.view = view;
    this.executor = executor;
    this.metric = model.getMetric().copy();
  }

  public void initializePresenter() {
    initializeFromState();
    setupHandlers();
    initializeFilterPanel();
  }

  private void setupHandlers() {
    view.getHistoryFunctionPanel().setRunAction(this::handleGroupFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
    view.getHistoryTimeRangeFunctionPanel().setRunAction(this::handleTimeRangeFunctionChange);
    view.getHistoryNormFunctionPanel().setRunAction(this::handleNormFunctionChange);

    view.getHistoryLegendPanel()
        .setStateChangeConsumer(showLegend -> handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());
  }

  private void initializeFilterPanel() {
    view.getHistoryFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.HISTORY);
  }

  private void initializeFromState() {
    ChartKey chartKey = model.getChartKey();

    GroupFunction historyGroupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
    if (historyGroupFunction != null) {
      metric.setGroupFunction(historyGroupFunction);
      metric.setChartType(GroupFunction.COUNT.equals(historyGroupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }
    if (ru.dimension.db.model.profile.cstype.CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryFunctionPanel().setEnabled(true, true, true);
    }
    view.getHistoryFunctionPanel().setSelected(metric.getGroupFunction());

    RangeHistory localHistoryRange = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory globalHistoryRange = UIState.INSTANCE.getHistoryRangeAll(component.name());
    RangeHistory effective = localHistoryRange != null ? localHistoryRange : globalHistoryRange;

    if (effective != null) {
      view.getHistoryRangePanel().setSelectedRange(effective);
    }

    if (effective == RangeHistory.CUSTOM) {
      ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(chartKey);
      if (customRange != null) {
        view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
        view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
      }
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey);
    if (showLegend != null) {
      view.getHistoryLegendPanel().setSelected(showLegend);
    }

    TimeRangeFunction timeRangeFunction = UIState.INSTANCE.getTimeRangeFunction(chartKey);
    if (timeRangeFunction != null) {
      metric.setTimeRangeFunction(timeRangeFunction);
    }
    view.getHistoryTimeRangeFunctionPanel().setSelected(metric.getTimeRangeFunction());

    NormFunction normFunction = UIState.INSTANCE.getNormFunction(chartKey);
    if (normFunction != null) {
      metric.setNormFunction(normFunction);
    }
    view.getHistoryNormFunctionPanel().setSelected(metric.getNormFunction());
  }

  public void initializeCharts() {
    createChart(null, null);
  }

  private void createChart(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                           Map<String, Color> seriesColorMap) {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          chart = createChartInstance(topMapSelected);
          if (seriesColorMap != null) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }
          chart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          SeriesType seriesTypeChart = chart.getSeriesType();

          if (SeriesType.COMMON.equals(seriesTypeChart)) {
            detail = buildDetail(chart, null, SeriesType.COMMON, null);
          } else {
            HistorySCP historySCP = (HistorySCP) chart;
            detail = buildDetail(chart, chart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getTopMapSelected());
          }

          ChartRange chartRange = getChartRange(chart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(model.getDStore(), metric, chartRange.getBegin(), chartRange.getEnd());

          view.getHistoryFilterPanel().clearFilterPanel();
          view.getHistoryFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());

          return () -> {
            view.getHistoryChartPanel().add(chart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(detail, BorderLayout.CENTER);

            boolean isCustom = chart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> log.info("Creating history chart complete")
    );
  }

  private SCP createChartInstance(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();

    ChartRange chartRange = getChartRange(config.getChartInfo());
    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());

    ProfileTaskQueryKey key = model.getKey();
    DStore dStore = model.getDStore();

    return new HistorySCP(dStore, config, key, topMapSelected);
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
    Metric metricCopy = metric.copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();

    // Group function -> chart type
    GroupFunction groupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
    if (groupFunction != null) {
      metricCopy.setGroupFunction(groupFunction);
      metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    // Range: prefer local, else global, default DAY
    RangeHistory local = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory global = UIState.INSTANCE.getHistoryRangeAll(component.name());
    chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(local,
                                                             Objects.requireNonNullElse(global, RangeHistory.DAY)));

    // Custom range handling: try state first, else read from UI
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

    // Extra functions
    TimeRangeFunction timeRangeFunction = UIState.INSTANCE.getTimeRangeFunction(chartKey);
    if (timeRangeFunction != null) {
      metricCopy.setTimeRangeFunction(timeRangeFunction);
    }
    NormFunction normFunction = UIState.INSTANCE.getNormFunction(chartKey);
    if (normFunction != null) {
      metricCopy.setNormFunction(normFunction);
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

  private DetailDashboardPanel buildDetail(SCP chart,
                                           Map<String, Color> initialSeriesColorMap,
                                           SeriesType seriesType,
                                           Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    Map<String, Color> seriesColorMapToUse = initialSeriesColorMap != null ?
        initialSeriesColorMap : chart.getSeriesColorMap();

    if (SeriesType.CUSTOM.equals(seriesType)) {
      seriesColorMapToUse = chart.getSeriesColorMap();
    }

    Metric chartMetric = chart.getConfig().getMetric();

    DetailDashboardPanel detailPanel =
        new DetailDashboardPanel(model.getChartKey(),
                                 model.getQueryInfo(),
                                 model.getChartInfo(),
                                 model.getTableInfo(),
                                 chartMetric,
                                 seriesColorMapToUse,
                                 Panel.HISTORY,
                                 seriesType,
                                 model.getDStore(),
                                 topMapSelected);

    CustomAction customAction = new CustomAction() {
      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
      }

      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                        Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        topMapSelected.values()
            .forEach(set -> set.forEach(val -> newSeriesColorMap.put(val, seriesColorMap.get(val))));

        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
        detailPanel.setSeriesType(SeriesType.CUSTOM);
      }

      @Override
      public void setBeginEnd(long begin, long end) {
      }
    };

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
    chart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  public void handleGroupFunctionChange(String action, GroupFunction function) {
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putHistoryGroupFunction(model.getChartKey(), function);

    view.getHistoryFilterPanel().clearFilterPanel();
    updateHistoryChart();
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);
    view.getHistoryRangePanel().setSelectedRange(range);
    updateHistoryChart();
  }

  private void handleTimeRangeFunctionChange(String action, TimeRangeFunction function) {
    UIState.INSTANCE.putTimeRangeFunction(model.getChartKey(), function);
    metric.setTimeRangeFunction(function);
    updateHistoryChart();
  }

  private void handleNormFunctionChange(String action, NormFunction function) {
    UIState.INSTANCE.putNormFunction(model.getChartKey(), function);
    metric.setNormFunction(function);
    updateHistoryChart();
  }

  public void updateHistoryChart() {
    updateChartInternal(null, null);
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

  private void updateLegendVisibility(boolean visibility) {
    if (chart != null) {
      view.getHistoryLegendPanel().setSelected(visibility);
      chart.getjFreeChart().getLegend().setVisible(visibility);
      chart.repaint();
    }
  }

  public void handleFilterChange(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                 Map<String, Color> seriesColorMap) {
    Map<String, Color> preservedColorMap = new HashMap<>(seriesColorMap != null ? seriesColorMap : Map.of());

    Map<CProfile, LinkedHashSet<String>> sanitizeTopMapSelected =
        FilterHelper.sanitizeTopMapSelected(topMapSelected, metric);

    updateChartInternal(preservedColorMap, sanitizeTopMapSelected);

    if (detail != null) {
      detail.updateSeriesColor(sanitizeTopMapSelected, preservedColorMap);
    }

    LogHelper.logMapSelected(sanitizeTopMapSelected);
  }

  private void updateChartInternal(Map<String, Color> seriesColorMap,
                                   Map<CProfile, LinkedHashSet<String>> topMapSelected) {
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

          chart = null;
          detail = null;

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }
          chart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(metric, seriesColorMap, topMapSelected);
            detail = buildDetail(chart, filterSeriesColorMap, SeriesType.CUSTOM, topMapSelected);
          } else {
            SeriesType seriesTypeChart = chart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              detail = buildDetail(chart, null, SeriesType.COMMON, null);
            } else {
              HistorySCP historySCP = (HistorySCP) chart;
              detail = buildDetail(chart, chart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getTopMapSelected());
            }
          }

          ChartRange chartRange = getChartRange(chart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(model.getDStore(), metric, chartRange.getBegin(), chartRange.getEnd());

          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());
          return () -> {
            view.getHistoryChartPanel().add(chart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(detail, BorderLayout.CENTER);

            boolean isCustom = chart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> log.info("Updating history chart complete")
    );
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

  private void applyCustomRange() {
    var rangePanel = view.getHistoryRangePanel();

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
    var rangePanel = view.getHistoryRangePanel();

    rangePanel.getButtonGroup().clearSelection();
    rangePanel.getCustom().setSelected(true);
    rangePanel.colorButton(RangeHistory.CUSTOM);

    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();

    return new ChartRange(from.getTime(), to.getTime());
  }
}