package ru.dimension.ui.component.module.adhoc.chart;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.AdHocChartKey;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
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

  public AdHocChartPresenter(AdHocChartModel model,
                             AdHocChartView view) {
    this.model = model;
    this.view = view;
    this.historyMetric = model.getMetric().copy();

    initializePresenter();
  }

  public void initializePresenter() {
    initializeFromState();

    view.getHistoryMetricFunctionPanel().setRunAction(this::handleHistoryMetricFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);

    view.getHistoryLegendPanel().setStateChangeConsumer(showLegend ->
                                                           handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());

    // Add filter panel initialization
    initializeFilterPanel();
  }

  public void initializeChart() {
    createHistoryChart();
  }

  private void createHistoryChart() {
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
          view.getHistoryFilterPanel().getMetric().setMetricFunction(
              historyChart.getConfig().getMetric().getMetricFunction()
          );

          SeriesType seriesTypeChart = historyChart.getSeriesType();

          if (SeriesType.COMMON.equals(seriesTypeChart)) {
            historyDetail = getDetail(historyChart, null, SeriesType.COMMON, null);
          } else {
            HistorySCP historySCP = (HistorySCP) historyChart;
            historyDetail = getDetail(historyChart, historyChart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getFilter());
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
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(Component.ADHOC.name()))
    );
  }

  private SCP createChart(Entry<CProfile, List<String>> filter) {
    ChartConfig config = buildChartConfig();
    DStore dStore = model.getDStore();

    ChartRange chartRange = getChartRangeAdHoc(config.getChartInfo());
    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());

    return new HistorySCP(dStore, config, null, filter);
  }

  protected DetailDashboardPanel getDetail(SCP chart,
                                           Map<String, Color> initialSeriesColorMap,
                                           SeriesType seriesType,
                                           Entry<CProfile, List<String>> filter) {
    ProcessType processType = ProcessType.HISTORY;
    Metric chartMetric = chart.getConfig().getMetric();

    Map<String, Color> seriesColorMapToUse = initialSeriesColorMap != null ?
        initialSeriesColorMap : chart.getSeriesColorMap();

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
                                 filter
        );

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
        detailPanel.updateSeriesColor(Map.entry(cProfileFilter, filter), newSeriesColorMap);
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

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    Metric metricCopy = model.getMetric().copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();
    AdHocKey key = model.getAdHocKey();

    // Get metric function from state
    MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(key);
    if (metricFunction != null) {
      metricCopy.setMetricFunction(metricFunction);
      metricCopy.setChartType(MetricFunction.COUNT.equals(metricFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    // Get range from state
    RangeHistory rangeHistory = UIState.INSTANCE.getHistoryRange(key);
    chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(rangeHistory, RangeHistory.DAY));

    // Handle custom range
    if (rangeHistory == RangeHistory.CUSTOM) {
      ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(key);
      if (customRange != null) {
        chartInfoCopy.setCustomBegin(customRange.getBegin());
        chartInfoCopy.setCustomEnd(customRange.getEnd());
      } else {
        ChartRange chartRange = getChartRangeFromHistoryRangePanel();
        chartInfoCopy.setCustomBegin(chartRange.getBegin());
        chartInfoCopy.setCustomEnd(chartRange.getEnd());
        UIState.INSTANCE.putHistoryCustomRange(key, chartRange);
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
    AdHocKey key = model.getAdHocKey();

    MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(key);
    if (metricFunction != null) {
      historyMetric.setMetricFunction(metricFunction);
      historyMetric.setChartType(MetricFunction.COUNT.equals(metricFunction) ?
                                     ChartType.STACKED : ChartType.LINEAR);
    }

    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryMetricFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryMetricFunctionPanel().setEnabled(true, true, true);
    }
    view.getHistoryMetricFunctionPanel().setSelected(historyMetric.getMetricFunction());

    RangeHistory localRange = UIState.INSTANCE.getHistoryRange(key);
    RangeHistory globalRange = UIState.INSTANCE.getHistoryRangeAll(Component.ADHOC.name());
    RangeHistory effectiveRange = localRange != null ? localRange : globalRange;

    if (effectiveRange != null) {
      view.getHistoryRangePanel().setSelectedRange(effectiveRange);
    }

    ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(key);
    if (customRange != null) {
      view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
      view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegend(key);
    if (showLegend != null) {
      view.getHistoryLegendPanel().setSelected(showLegend);
    }
  }

  private void handleHistoryMetricFunctionChange(String action, MetricFunction function) {
    UIState.INSTANCE.putHistoryMetricFunction(model.getAdHocKey(), function);
    updateHistoryChart(null, null);
  }

  private void updateHistoryChart(Map<String, Color> seriesColorMap, Entry<CProfile, List<String>> filter) {
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

          historyChart = createChart(filter);

          if (seriesColorMap != null) {
            historyChart.loadSeriesColor(model.getMetric(), seriesColorMap);
          }
          historyChart.initialize();

          if (seriesColorMap != null && filter != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(seriesColorMap, filter.getValue());
            historyDetail = getDetail(historyChart, filterSeriesColorMap, SeriesType.CUSTOM, filter);
          } else {
            SeriesType seriesTypeChart = historyChart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              historyDetail = getDetail(historyChart, null, SeriesType.COMMON, null);
            } else {
              HistorySCP historySCP = (HistorySCP) historyChart;
              historyDetail = getDetail(historyChart, historyChart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getFilter());
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
          view.getHistoryFilterPanel().getMetric().setMetricFunction(
              historyChart.getConfig().getMetric().getMetricFunction()
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
        () -> setDetailState(PanelTabType.HISTORY, UIState.INSTANCE.getShowDetailAll(Component.ADHOC.name()))
    );
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getAdHocKey(), range);
    model.getChartInfo().setRangeHistory(range);
    view.getHistoryRangePanel().setSelectedRange(range);
    updateHistoryChart(null, null);
  }

  public void updateHistoryRange(RangeHistory range) {
    view.getHistoryRangePanel().setSelectedRange(range);
    handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    UIState.INSTANCE.putHistoryRange(model.getAdHocKey(), RangeHistory.CUSTOM);
    UIState.INSTANCE.putHistoryCustomRange(model.getAdHocKey(), range);

    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    view.getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(range.getBegin()));
    view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(range.getEnd()));
    updateHistoryChart(null, null);
  }

  public void setDetailState(DetailState detailState) {
    boolean showDetail = detailState == DetailState.SHOW;
    view.setHistoryDetailVisible(showDetail);
    if (historyChart != null) {
      historyChart.clearSelectionRegion();
    }
  }

  public void setDetailState(PanelTabType panelTabType,
                             DetailState detailState) {
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
    UIState.INSTANCE.putShowLegend(model.getAdHocKey(), visibility);
  }

  private void applyCustomRange() {
    HistoryRangePanel rangePanel = view.getHistoryRangePanel();
    rangePanel.getButtonGroup().clearSelection();
    rangePanel.getCustom().setSelected(true);
    rangePanel.colorButton(RangeHistory.CUSTOM);

    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();
    ChartRange chartRange = new ChartRange(from.getTime(), to.getTime());

    UIState.INSTANCE.putHistoryCustomRange(model.getAdHocKey(), chartRange);
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

  @Override
  public void receive(Message message) {
    log.info("Message received: " + message.action());
    PanelTabType panelTabType = PanelTabType.HISTORY;
    CProfile cProfileFilter = message.parameters().get("cProfileFilter");
    List<String> filter = message.parameters().get("filter");
    Map<String, Color> seriesColorMap = message.parameters().get("seriesColorMap");

    switch (message.action()) {
      case ADD_CHART_FILTER, REMOVE_CHART_FILTER ->
          handleFilterChange(panelTabType, cProfileFilter, filter, seriesColorMap);
    }
  }

  private Map<String, Color> getFilterSeriesColorMap(Map<String, Color> seriesColorMap, List<String> filterKeys) {
    if (filterKeys == null || filterKeys.isEmpty()) {
      return seriesColorMap;
    }

    Map<String, Color> filteredMap = new HashMap<>();
    for (String key : filterKeys) {
      if (seriesColorMap.containsKey(key)) {
        filteredMap.put(key, seriesColorMap.get(key));
      }
    }
    return filteredMap;
  }

  private void handleFilterChange(PanelTabType panelTabType,
                                  CProfile cProfileFilter,
                                  List<String> filter,
                                  Map<String, Color> seriesColorMap) {
    Map<String, Color> preservedColorMap = new HashMap<>(seriesColorMap);
    Entry<CProfile, List<String>> filterEntry =
        (filter == null || filter.isEmpty()) ? null : Map.entry(cProfileFilter, filter);

    if (panelTabType == PanelTabType.HISTORY) {
      updateHistoryChart(preservedColorMap, filterEntry);
    }

    if (panelTabType == PanelTabType.HISTORY && historyDetail != null) {
      historyDetail.updateSeriesColor(Map.entry(cProfileFilter, filter != null ? filter : Collections.emptyList()), preservedColorMap);
    }
  }
}