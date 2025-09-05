package ru.dimension.ui.component.module.adhoc.chart;

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
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
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
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.helper.LogHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.AdHocChartKey;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.AdHocStateManager;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class AdHocChartPresenter implements HelperChart, MessageAction {

  private final AdHocChartModel model;
  private final AdHocChartView view;

  private SCP historyChart;
  private DetailDashboardPanel historyDetail;

  @Getter
  private final Metric historyMetric;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  public AdHocChartPresenter(AdHocChartModel model, AdHocChartView view) {
    this.model = model;
    this.view = view;
    this.historyMetric = model.getMetric().copy();
    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getHistoryFunctionPanel().setRunAction(this::handleHistoryGroupFunctionChange);
    view.getHistoryTimeRangeFunctionPanel().setRunAction(this::handleHistoryTimeRangeFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
    view.getHistoryNormFunctionPanel().setRunAction(this::handleHistoryNormFunctionChange);

    view.getHistoryLegendPanel().setStateChangeConsumer(
        showLegend -> handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());

    initializeFilterPanel();
  }

  public void initializeChart() {
    createHistoryChart();
  }

  private void createHistoryChart() {
    AdHocKey adHocKey = model.getAdHocKey();
    String globalKey = KeyHelper.getGlobalKey(adHocKey.getConnectionId(), model.getTableInfo().getTableName());

    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          historyChart = createChart(null);
          historyChart.initialize();

          // Add filter panel data source setup
          ChartRange chartRange = getChartRangeAdHoc(historyChart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(
              model.getDStore(),
              model.getMetric(),
              chartRange.getBegin(),
              chartRange.getEnd()
          );

          view.getHistoryFilterPanel().clearFilterPanel();
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(
              historyChart.getConfig().getMetric().getGroupFunction()
          );

          SeriesType seriesTypeChart = historyChart.getSeriesType();

          if (SeriesType.COMMON.equals(seriesTypeChart)) {
            historyDetail = getDetail(historyChart, null, SeriesType.COMMON, null);
          } else {
            HistorySCP historySCP = (HistorySCP) historyChart;
            historyDetail = getDetail(historyChart,
                                      historyChart.getSeriesColorMap(),
                                      SeriesType.CUSTOM,
                                      historySCP.getTopMapSelected());
          }

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> {
          setDetailState(PanelTabType.HISTORY, adHocStateManager.getShowDetailAll(globalKey));
          updateLegendVisibility(adHocStateManager.getShowLegend(adHocKey, globalKey));
        }
    );
  }

  private SCP createChart(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();
    DStore dStore = model.getDStore();

    ChartRange chartRange = getChartRangeAdHoc(config.getChartInfo());
    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());

    // TODO implement code for adhoc
    ProfileTaskQueryKey key = new ProfileTaskQueryKey(0, 0, 0);

    return new HistorySCP(dStore, config, key, topMapSelected);
  }

  protected DetailDashboardPanel getDetail(SCP chart,
                                           Map<String, Color> initialSeriesColorMap,
                                           SeriesType seriesType,
                                           Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ProcessType processType = ProcessType.HISTORY;
    Metric chartMetric = chart.getConfig().getMetric();

    Map<String, Color> seriesColorMapToUse =
        initialSeriesColorMap != null ? initialSeriesColorMap : chart.getSeriesColorMap();

    if (SeriesType.CUSTOM.equals(seriesType) && seriesColorMapToUse.isEmpty()) {
      seriesColorMapToUse = chart.getSeriesColorMap();
    }

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
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
      }

      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                        Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        topMapSelected.values()
            .forEach(set -> set.forEach(
                value -> newSeriesColorMap.put(value, seriesColorMap.get(value))));

        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
      }

      @Override
      public void setBeginEnd(long begin, long end) {
      }
    };

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
    chart.addChartListenerReleaseMouse(detailPanel);
    return detailPanel;
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    Metric metricCopy = model.getMetric().copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();
    AdHocKey key = model.getAdHocKey();
    String globalKey = KeyHelper.getGlobalKey(key.getConnectionId(), model.getTableInfo().getTableName());

    GroupFunction groupFunction = adHocStateManager.getHistoryGroupFunction(key);
    if (groupFunction != null) {
      metricCopy.setGroupFunction(groupFunction);
      metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction)
                                  ? ChartType.STACKED : ChartType.LINEAR);
    }

    TimeRangeFunction timeRangeFunction = adHocStateManager.getTimeRangeFunction(key);
    if (timeRangeFunction != null) {
      metricCopy.setTimeRangeFunction(timeRangeFunction);
    }

    NormFunction normFunction = adHocStateManager.getNormFunction(key);
    if (normFunction != null) {
      metricCopy.setNormFunction(normFunction);
    }

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(key, globalKey);
    chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(rangeHistory, RangeHistory.DAY));

    if (rangeHistory == RangeHistory.CUSTOM) {
      ChartRange customRange = adHocStateManager.getCustomChartRange(key, globalKey);
      if (customRange == null) {
        customRange = adHocStateManager.getCustomChartRange(key, globalKey);
      }
      if (customRange != null) {
        chartInfoCopy.setCustomBegin(customRange.getBegin());
        chartInfoCopy.setCustomEnd(customRange.getEnd());
      } else {
        ChartRange chartRange = getChartRangeFromHistoryRangePanel();
        chartInfoCopy.setCustomBegin(chartRange.getBegin());
        chartInfoCopy.setCustomEnd(chartRange.getEnd());
        adHocStateManager.putHistoryCustomRange(key, chartRange);
      }
    }

    config.setTitle("");
    config.setXAxisLabel(model.getMetric().getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metricCopy);
    config.setChartInfo(chartInfoCopy);
    config.setQueryInfo(model.getQueryInfo());

    return config;
  }

  private void initializeFromState() {
    AdHocKey adHocKey = model.getAdHocKey();
    String tableName = model.getTableInfo().getTableName();
    String globalKey = KeyHelper.getGlobalKey(adHocKey.getConnectionId(), tableName);

    GroupFunction groupFunction = adHocStateManager.getHistoryGroupFunction(adHocKey);
    if (groupFunction != null) {
      historyMetric.setGroupFunction(groupFunction);
      historyMetric.setChartType(GroupFunction.COUNT.equals(groupFunction)
                                     ? ChartType.STACKED : ChartType.LINEAR);
    }

    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryFunctionPanel().setEnabled(true, true, true);
    }
    view.getHistoryFunctionPanel().setSelected(historyMetric.getGroupFunction());

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(adHocKey, globalKey);
    if (rangeHistory != null) {
      view.getHistoryRangePanel().setSelectedRange(rangeHistory);
    }

    ChartRange customRange = adHocStateManager.getCustomChartRange(adHocKey, globalKey);
    if (customRange != null) {
      view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
      view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
    }

    Boolean showLegend = adHocStateManager.getShowLegend(adHocKey, globalKey);
    if (showLegend != null) {
      view.getHistoryLegendPanel().setSelected(showLegend);
    }

    TimeRangeFunction timeRangeFunction = adHocStateManager.getTimeRangeFunction(adHocKey);
    if (timeRangeFunction != null) {
      historyMetric.setTimeRangeFunction(timeRangeFunction);
    }
    view.getHistoryTimeRangeFunctionPanel().setSelected(historyMetric.getTimeRangeFunction());

    NormFunction normFunction = adHocStateManager.getNormFunction(adHocKey);
    if (normFunction != null) {
      historyMetric.setNormFunction(normFunction);
    }
    view.getHistoryNormFunctionPanel().setSelected(historyMetric.getNormFunction());
  }

  private void handleHistoryGroupFunctionChange(String action, GroupFunction function) {
    adHocStateManager.putHistoryGroupFunction(model.getAdHocKey(), function);
    updateHistoryChart(null, null);
  }

  private void updateHistoryChart(Map<String, Color> seriesColorMap,
                                  Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    AdHocKey adHocKey = model.getAdHocKey();
    String globalKey = KeyHelper.getGlobalKey(adHocKey.getConnectionId(), model.getTableInfo().getTableName());

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

          historyChart = createChart(topMapSelected);

          if (seriesColorMap != null) {
            historyChart.loadSeriesColor(model.getMetric(), seriesColorMap);
          }
          historyChart.initialize();

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap =
                getFilterSeriesColorMap(historyMetric, seriesColorMap, topMapSelected);
            historyDetail = getDetail(historyChart,
                                      filterSeriesColorMap,
                                      SeriesType.CUSTOM,
                                      topMapSelected);
          } else {
            SeriesType seriesTypeChart = historyChart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              historyDetail = getDetail(historyChart, null, SeriesType.COMMON, null);
            } else {
              HistorySCP historySCP = (HistorySCP) historyChart;
              historyDetail = getDetail(historyChart,
                                        historyChart.getSeriesColorMap(),
                                        SeriesType.CUSTOM,
                                        historySCP.getTopMapSelected());
            }
          }

          ChartRange chartRange = getChartRangeAdHoc(historyChart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(
              model.getDStore(),
              model.getMetric(),
              chartRange.getBegin(),
              chartRange.getEnd()
          );

          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(historyChart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(
              historyChart.getConfig().getMetric().getGroupFunction()
          );

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);

            boolean isCustom = historyChart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> setDetailState(PanelTabType.HISTORY, adHocStateManager.getShowDetailAll(globalKey))
    );
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    adHocStateManager.putHistoryRange(model.getAdHocKey(), range);
    model.getChartInfo().setRangeHistory(range);
    view.getHistoryRangePanel().setSelectedRange(range);
    updateHistoryChart(null, null);
  }

  private void handleHistoryNormFunctionChange(String action, NormFunction function) {
    adHocStateManager.putNormFunction(model.getAdHocKey(), function);
    model.getMetric().setNormFunction(function);
    view.getHistoryNormFunctionPanel().setSelected(function);
    updateHistoryChart(null, null);
  }

  public void updateHistoryRange(RangeHistory range) {
    view.getHistoryRangePanel().setSelectedRange(range);
    handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange chartRange) {
    adHocStateManager.putHistoryRange(model.getAdHocKey(), RangeHistory.CUSTOM);
    adHocStateManager.putHistoryCustomRange(model.getAdHocKey(), chartRange);

    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    view.getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(chartRange.getBegin()));
    view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(chartRange.getEnd()));
    updateHistoryChart(null, null);
  }

  public void setDetailState(DetailState detailState) {
    boolean showDetail = detailState == DetailState.SHOW;
    view.setHistoryDetailVisible(showDetail);
    if (historyChart != null) {
      historyChart.clearSelectionRegion();
    }
  }

  public void setDetailState(PanelTabType panelTabType, DetailState detailState) {
    if (panelTabType == PanelTabType.HISTORY) {
      boolean showDetail = detailState == DetailState.SHOW;
      view.setHistoryDetailVisible(showDetail);
      if (historyChart != null) {
        historyChart.clearSelectionRegion();
      }
    }
  }

  public void handleLegendChange(Boolean showLegend) {
    boolean visibility = showLegend != null ? showLegend : true;
    updateLegendVisibility(visibility);
    adHocStateManager.putShowLegend(model.getAdHocKey(), visibility);
  }

  private void applyCustomRange() {
    HistoryRangePanel rangePanel = view.getHistoryRangePanel();
    rangePanel.getButtonGroup().clearSelection();
    rangePanel.getCustom().setSelected(true);
    rangePanel.colorButton(RangeHistory.CUSTOM);

    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();
    ChartRange chartRange = new ChartRange(from.getTime(), to.getTime());

    adHocStateManager.putHistoryCustomRange(model.getAdHocKey(), chartRange);
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

  private void initializeFilterPanel() {
    view.getHistoryFilterPanel().initializeChartPanel(
        new AdHocChartKey(model.getAdHocKey()),
        model.getTableInfo(),
        Panel.HISTORY
    );
  }

  private void handleHistoryTimeRangeFunctionChange(String action, TimeRangeFunction function) {
    adHocStateManager.putTimeRangeFunction(model.getAdHocKey(), function);
    log.info("Action: " + action + " for time range function: " + function);

    handleFilterChange(PanelTabType.HISTORY,
                       model.getTopMapSelected().isEmpty() ? null : model.getTopMapSelected(),
                       model.getSeriesColorMap());
  }

  @Override
  public void receive(Message message) {
    log.info("Message received >>> " + message.destination() + " with action >>> " + message.action());
    PanelTabType panelTabType = PanelTabType.HISTORY;

    Map<CProfile, LinkedHashSet<String>> topMapSelected = message.parameters().get("topMapSelected");
    Map<String, Color> seriesColorMap = message.parameters().get("seriesColorMap");

    model.updateMaps(topMapSelected, seriesColorMap);
    LogHelper.logMapSelected(topMapSelected);

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
          FilterHelper.sanitizeTopMapSelected(topMapSelected, historyMetric);
      updateHistoryChart(preservedColorMap, sanitizeTopMapSelected);
    }

    if (panelTabType == PanelTabType.HISTORY && historyDetail != null) {
      Map<CProfile, LinkedHashSet<String>> sanitizeTopMapSelected =
          FilterHelper.sanitizeTopMapSelected(topMapSelected, historyMetric);
      historyDetail.updateSeriesColor(sanitizeTopMapSelected, preservedColorMap);
    }
  }
}