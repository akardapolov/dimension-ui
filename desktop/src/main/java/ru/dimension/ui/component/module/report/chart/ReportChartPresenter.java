package ru.dimension.ui.component.module.report.chart;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
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
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class ReportChartPresenter implements HelperChart, MessageAction {
  private final ReportChartModel model;
  private final ReportChartView view;

  private SCP historyChart;
  private DetailDashboardPanel historyDetail;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ReportChartPresenter(ReportChartModel model,
                              ReportChartView view) {
    this.model = model;
    this.view = view;

    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getHistoryTimeRangeFunctionPanel().setRunAction(this::handleHistoryTimeRangeFunctionChange);
    view.getHistoryNormFunctionPanel().setRunAction(this::handleHistoryNormFunctionChange);
    view.getHistoryFunctionPanel().setRunAction(this::handleHistoryGroupFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);

    view.getHistoryLegendPanel().setStateChangeConsumer(showLegend ->
                                                            handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());

    initializeFilterPanels();
  }

  public void initializeCharts() {
    createHistoryChart();
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
          view.getHistoryFilterPanel()
              .setDataSource(model.getDStore(), model.getMetric(), chartRange.getBegin(), chartRange.getEnd());

          view.getHistoryFilterPanel().clearFilterPanel();
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric()
              .setGroupFunction(historyChart.getConfig().getMetric().getGroupFunction());
          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(model.getComponent().name()))
    );
  }

  private SCP createChart(AnalyzeType analyzeType,
                          Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig(analyzeType);
    ProfileTaskQueryKey key = model.getKey();
    DStore dStore = model.getDStore();

    if (analyzeType == AnalyzeType.HISTORY) {
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
    ProcessType processType = ProcessType.HISTORY;

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
                                 model.getChartInfo(),
                                 model.getTableInfo(),
                                 chartMetric,
                                 seriesColorMapToUse,
                                 processType,
                                 seriesType,
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
            .forEach(set -> set.forEach(value -> newSeriesColorMap.put(value, seriesColorMap.get(value))));

        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
        detailPanel.setSeriesType(SeriesType.CUSTOM);
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

    if (AnalyzeType.HISTORY.equals(analyzeType)) {
      GroupFunction groupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
      if (groupFunction != null) {
        metricCopy.setGroupFunction(groupFunction);
        metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
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

      TimeRangeFunction timeRangeFunction = UIState.INSTANCE.getTimeRangeFunction(chartKey);
      if (timeRangeFunction != null) {
        metricCopy.setTimeRangeFunction(timeRangeFunction);
      }

      NormFunction normFunction = UIState.INSTANCE.getNormFunction(chartKey);
      if (normFunction != null) {
        metricCopy.setNormFunction(normFunction);
      }
    }

    if (AnalyzeType.HISTORY.equals(analyzeType)) {
      RangeHistory localRange = UIState.INSTANCE.getHistoryRange(chartKey);
      RangeHistory globalRange = UIState.INSTANCE.getHistoryRangeAll(model.getComponent().name());
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

    RangeHistory inputRangeHistory = model.getChartInfo().getRangeHistory();
    if (inputRangeHistory != null) {
      UIState.INSTANCE.putHistoryRange(chartKey, inputRangeHistory);
      if (inputRangeHistory == RangeHistory.CUSTOM) {
        long customBegin = model.getChartInfo().getCustomBegin();
        long customEnd = model.getChartInfo().getCustomEnd();
        UIState.INSTANCE.putHistoryCustomRange(chartKey, new ChartRange(customBegin, customEnd));
      }
    }

    GroupFunction historyGroupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
    if (historyGroupFunction != null) {
      model.getMetric().setGroupFunction(historyGroupFunction);
      model.getMetric().setChartType(
          GroupFunction.COUNT.equals(historyGroupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }
    view.getHistoryFunctionPanel().setSelected(model.getMetric().getGroupFunction());

    RangeHistory localHistoryRange = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory globalHistoryRange = UIState.INSTANCE.getHistoryRangeAll(model.getComponent().name());
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
      view.getHistoryLegendPanel().setSelected(showLegend);
    }

    TimeRangeFunction timeRangeFunction = UIState.INSTANCE.getTimeRangeFunction(model.getChartKey());
    if (timeRangeFunction != null) {
      model.getMetric().setTimeRangeFunction(timeRangeFunction);
    }
    view.getHistoryTimeRangeFunctionPanel().setSelected(model.getMetric().getTimeRangeFunction());

    NormFunction normFunction = UIState.INSTANCE.getNormFunction(model.getChartKey());
    if (normFunction != null) {
      model.getMetric().setNormFunction(normFunction);
    }
    view.getHistoryNormFunctionPanel().setSelected(model.getMetric().getNormFunction());
  }

  private void handleHistoryGroupFunctionChange(String action,
                                                GroupFunction function) {
    model.getMetric().setGroupFunction(function);
    model.getMetric().setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putHistoryGroupFunction(model.getChartKey(), function);

    view.getHistoryFilterPanel().clearFilterPanel();
    updateHistoryChart();
  }

  public void handleHistoryRangeChange(String action,
                                       RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);

    view.getHistoryRangePanel().setSelectedRange(range);

    updateHistoryChart();
  }

  private void handleHistoryTimeRangeFunctionChange(String action, TimeRangeFunction function) {
    UIState.INSTANCE.putTimeRangeFunction(model.getChartKey(), function);
    model.getMetric().setTimeRangeFunction(function);
    updateHistoryChart();
  }

  private void handleHistoryNormFunctionChange(String action, NormFunction function) {
    UIState.INSTANCE.putNormFunction(model.getChartKey(), function);
    model.getMetric().setNormFunction(function);
    updateHistoryChart();
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

    //view.setHistoryDetailVisible(showDetail);
    //historyChart.clearSelectionRegion();
  }

  public void setDetailState(PanelTabType panelTabType,
                             DetailState detailState) {
    boolean showDetail = detailState == DetailState.SHOW;
    log.info("Setting detail visibility to: {} for chart {}", showDetail, model.getChartKey());

    //view.setHistoryDetailVisible(showDetail);
    //historyChart.clearSelectionRegion();
  }

  public void handleLegendChangeAll(Boolean showLegend) {
    Boolean showLegendAll = UIState.INSTANCE.getShowLegendAll(model.getComponent().name());
    boolean effectiveVisibility = (showLegendAll != null && showLegendAll) ||
        (showLegend != null ? showLegend : false);

    updateLegendVisibility(effectiveVisibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), effectiveVisibility);
  }

  public void handleLegendChange(Boolean showLegend) {
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
    if (historyChart != null) {
      view.getHistoryLegendPanel().setSelected(visibility);
      historyChart.getjFreeChart().getLegend().setVisible(visibility);
      historyChart.repaint();
    }
  }

  public void initializeFilterPanels() {
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

    if (panelTabType == PanelTabType.HISTORY) {
      Map<CProfile, LinkedHashSet<String>> sanitizeTopMapSelected =
          FilterHelper.sanitizeTopMapSelected(topMapSelected, model.getMetric());
      updateHistoryChart(preservedColorMap, sanitizeTopMapSelected);
    }

    if (panelTabType == PanelTabType.HISTORY && historyDetail != null) {
      Map<CProfile, LinkedHashSet<String>> sanitizeTopMapSelected =
          FilterHelper.sanitizeTopMapSelected(topMapSelected, model.getMetric());
      historyDetail.updateSeriesColor(sanitizeTopMapSelected, preservedColorMap);
    }
  }

  private void updateHistoryChart() {
    updateHistoryChart(null, null);
  }

  private void updateHistoryChart(Map<String, Color> seriesColorMap,
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

          historyChart = null;
          historyDetail = null;

          historyChart = createChart(AnalyzeType.HISTORY, topMapSelected);

          if (seriesColorMap != null) {
            historyChart.loadSeriesColor(model.getMetric(), seriesColorMap);
          }
          historyChart.initialize();

          handleLegendChangeAll(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(model.getMetric(), seriesColorMap, topMapSelected);
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
          view.getHistoryFilterPanel()
              .setDataSource(model.getDStore(), model.getMetric(), chartRange.getBegin(), chartRange.getEnd());

          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric()
              .setGroupFunction(historyChart.getConfig().getMetric().getGroupFunction());
          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(model.getComponent().name()))
    );
  }
}