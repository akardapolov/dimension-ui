package ru.dimension.ui.component.module.adhoc.chart.unit;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistoryAdHocSCP;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.adhoc.chart.AdHocChartModel;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.module.api.UnitPresenter;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.KeyHelper;
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
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.AdHocStateManager;
import ru.dimension.ui.view.detail.DetailAdHocPanel;

@Log4j2
public class AdHocHistoryUnitPresenter implements UnitPresenter, HelperChart {

  @Getter private final AdHocChartModel model;
  @Getter private final AdHocHistoryUnitView view;
  @Getter private final ExecutorService executor;

  @Getter
  private SCP chart;
  private DetailAdHocPanel detail;

  @Getter
  private final Metric metric;
  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  public AdHocHistoryUnitPresenter(AdHocChartModel model, AdHocHistoryUnitView view, ExecutorService executor) {
    this.model = model;
    this.view = view;
    this.executor = executor;
    this.metric = model.getMetric().copy();
  }

  @Override
  public void initializePresenter() {
    initializeFromState();
    setupHandlers();
    initializeFilterPanel();
  }

  @Override
  public void initializeCharts() {
    updateChartInternal(null, null);
  }

  @Override
  public void updateChart() {
    updateChartInternal(null, null);
  }

  @Override
  public void handleLegendChangeAll(Boolean showLegend) {
    boolean visibility = showLegend != null && showLegend;
    updateLegendVisibility(visibility);
    adHocStateManager.putShowLegend(model.getAdHocKey(), visibility);
  }

  @Override
  public void handleFilterChange(Map<CProfile, LinkedHashSet<String>> topMapSelected, Map<String, Color> seriesColorMap) {
    Map<String, Color> preservedColorMap = new HashMap<>(seriesColorMap != null ? seriesColorMap : Map.of());
    updateChartInternal(preservedColorMap, topMapSelected);

    if (detail != null) {
      detail.updateSeriesColor(topMapSelected, preservedColorMap);
    }
  }

  private void setupHandlers() {
    view.getHistoryFunctionPanel().setRunAction(this::handleGroupFunctionChange);
    view.getHistoryTimeRangeFunctionPanel().setRunAction(this::handleTimeRangeFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
    view.getHistoryNormFunctionPanel().setRunAction(this::handleNormFunctionChange);
    view.getHistoryLegendPanel().setStateChangeConsumer(show -> handleLegendChangeAll(ChartLegendState.SHOW.equals(show)));
    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());
  }

  private void initializeFilterPanel() {
    view.getHistoryFilterPanel().initializeChartPanel(
        new AdHocChartKey(model.getAdHocKey()),
        model.getTableInfo(),
        MessageBroker.Panel.HISTORY
    );
  }

  private void initializeFromState() {
    AdHocKey adHocKey = model.getAdHocKey();
    String tableName = model.getTableInfo().getTableName();
    String globalKey = KeyHelper.getGlobalKey(adHocKey.getConnectionId(), tableName);

    GroupFunction groupFunction = adHocStateManager.getHistoryGroupFunction(adHocKey);
    if (groupFunction != null) {
      metric.setGroupFunction(groupFunction);
      metric.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryFunctionPanel().setEnabled(true, true, true);
    }
    view.getHistoryFunctionPanel().setSelected(metric.getGroupFunction());

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
      metric.setTimeRangeFunction(timeRangeFunction);
    }
    view.getHistoryTimeRangeFunctionPanel().setSelected(metric.getTimeRangeFunction());

    NormFunction normFunction = adHocStateManager.getNormFunction(adHocKey);
    if (normFunction != null) {
      metric.setNormFunction(normFunction);
    }
    view.getHistoryNormFunctionPanel().setSelected(metric.getNormFunction());
  }

  private void updateChartInternal(Map<String, Color> seriesColorMap, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    SwingTaskRunner.runWithProgress(
        view.getChartPanel(),
        executor,
        () -> {
          clearChartAndDetailPanels();

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null) {
            chart.loadSeriesColor(model.getMetric(), seriesColorMap);
          }
          chart.initialize();

          handleLegendChangeAll(adHocStateManager.getShowLegend(model.getAdHocKey(),
                                                                KeyHelper.getGlobalKey(model.getAdHocKey().getConnectionId(), model.getTableInfo().getTableName())));

          if (seriesColorMap != null && topMapSelected != null) {
            Map<String, Color> filterSeriesColorMap = getFilterSeriesColorMap(metric, seriesColorMap, topMapSelected);
            detail = buildDetail(chart, filterSeriesColorMap, SeriesType.CUSTOM, topMapSelected);
          } else {
            SeriesType seriesTypeChart = chart.getSeriesType();
            if (SeriesType.COMMON.equals(seriesTypeChart)) {
              detail = buildDetail(chart, null, SeriesType.COMMON, null);
            } else {
              HistoryAdHocSCP historySCP = (HistoryAdHocSCP) chart;
              detail = buildDetail(chart, chart.getSeriesColorMap(), SeriesType.CUSTOM, historySCP.getTopMapSelected());
            }
          }

          ChartRange chartRange = getChartRangeAdHoc(chart.getConfig().getChartInfo());
          view.getHistoryFilterPanel().setDataSource(model.getDStore(), metric, chartRange.getBegin(), chartRange.getEnd());
          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());

          return () -> {
            addChartAndDetailToPanels();
            boolean isCustom = chart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating/updating ad-hoc history chart", e),
        () -> createProgressBar("Creating/updating ad-hoc history chart..."),
        () -> log.info("Creating/updating ad-hoc history chart complete")
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

  private SCP createChartInstance(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();
    DStore dStore = model.getDStore();
    ChartRange chartRange = getChartRangeAdHoc(config.getChartInfo());
    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());
    ProfileTaskQueryKey key = new ProfileTaskQueryKey(0, 0, 0);
    return new HistoryAdHocSCP(dStore, config, key, topMapSelected);
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    Metric metricCopy = metric.copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();
    AdHocKey key = model.getAdHocKey();
    String globalKey = KeyHelper.getGlobalKey(key.getConnectionId(), model.getTableInfo().getTableName());

    GroupFunction groupFunction = adHocStateManager.getHistoryGroupFunction(key);
    if (groupFunction != null) {
      metricCopy.setGroupFunction(groupFunction);
      metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    TimeRangeFunction timeRangeFunction = adHocStateManager.getTimeRangeFunction(key);
    if(timeRangeFunction != null) {
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

  private DetailAdHocPanel buildDetail(SCP chart, Map<String, Color> initialSeriesColorMap, SeriesType seriesType, Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    Map<String, Color> seriesColorMapToUse = initialSeriesColorMap != null ? initialSeriesColorMap : chart.getSeriesColorMap();
    if (SeriesType.CUSTOM.equals(seriesType) && seriesColorMapToUse.isEmpty()) {
      seriesColorMapToUse = chart.getSeriesColorMap();
    }

    DetailAdHocPanel detailPanel = new DetailAdHocPanel(
        model.getDStore(), model.getQueryInfo(), model.getChartInfo(), model.getTableInfo(),
        chart.getConfig().getMetric(), seriesColorMapToUse, MessageBroker.Panel.HISTORY,
        seriesType, model.getAdHocKey(), topMapSelected
    );

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, new CustomAction() {
      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected, Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        topMapSelected.values().forEach(set -> set.forEach(val -> newSeriesColorMap.put(val, seriesColorMap.get(val))));
        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
      }
      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {}
      @Override
      public void setBeginEnd(long begin, long end) {}
    }));
    chart.addChartListenerReleaseMouse(detailPanel);
    return detailPanel;
  }

  public void handleGroupFunctionChange(String action, GroupFunction function) {
    adHocStateManager.putHistoryGroupFunction(model.getAdHocKey(), function);
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    updateChart();
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    adHocStateManager.putHistoryRange(model.getAdHocKey(), range);
    model.getChartInfo().setRangeHistory(range);
    updateChart();
  }

  public void handleTimeRangeFunctionChange(String action, TimeRangeFunction function) {
    adHocStateManager.putTimeRangeFunction(model.getAdHocKey(), function);
    metric.setTimeRangeFunction(function);
    updateChart();
  }

  public void handleNormFunctionChange(String action, NormFunction function) {
    adHocStateManager.putNormFunction(model.getAdHocKey(), function);
    metric.setNormFunction(function);
    updateChart();
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
    Date from = rangePanel.getDateTimePickerFrom().getDate();
    Date to = rangePanel.getDateTimePickerTo().getDate();
    return new ChartRange(from.getTime(), to.getTime());
  }

  private void updateLegendVisibility(boolean visibility) {
    if (chart != null) {
      view.getHistoryLegendPanel().setSelected(visibility);
      if (chart.getjFreeChart().getLegend() != null) {
        chart.getjFreeChart().getLegend().setVisible(visibility);
      }
      chart.repaint();
    }
  }

  private void clearChartAndDetailPanels() {
    view.getChartPanel().removeAll();
    view.getDetailPanel().ifPresent(JPanel::removeAll);
    view.getChartPanel().revalidate();
    view.getChartPanel().repaint();
    view.getDetailPanel().ifPresent(p -> {
      p.revalidate();
      p.repaint();
    });
  }

  private void addChartAndDetailToPanels() {
    view.getChartPanel().add(chart, BorderLayout.CENTER);
    view.getDetailPanel().ifPresent(p -> p.add(detail, BorderLayout.CENTER));
  }
}