package ru.dimension.ui.component.module.chart.main.unit;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.base.BaseUnitPresenter;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.component.panel.LegendPanel;
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

@Log4j2
public class HistoryUnitPresenter extends BaseUnitPresenter<HistoryUnitView> implements HelperChart {

  private volatile boolean initialized = false;

  public HistoryUnitPresenter(MessageBroker.Component component,
                              ChartModel model,
                              HistoryUnitView view,
                              ExecutorService executor) {
    super(component, model, view, executor);
  }

  @Override
  public void initializePresenter() {
    initializeFromState();
    setupHandlers();
    initializeFilterPanel();
  }

  public void initializeIfNeeded() {
    if (initialized) {
      return;
    }
    synchronized (this) {
      if (initialized) {
        return;
      }
      log.info("Lazy initializing HISTORY tab...");
      initializePresenter();
      initializeCharts();
      initialized = true;
    }
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
    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
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

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey, component.name(), ChartLegendState.SHOW);
    view.getHistoryLegendPanel().setSelected(showLegend);

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

  @Override
  public void initializeCharts() {
    updateChartInternal(null, null);
  }

  @Override
  public void updateChart() {
    updateChartInternal(null, null);
  }

  @Override
  protected LegendPanel getLegendPanel() {
    return view.getHistoryLegendPanel();
  }

  @Override
  protected Panel getPanelType() {
    return Panel.HISTORY;
  }

  @Override
  protected void updateChartInternal(Map<String, Color> seriesColorMap,
                                     Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          clearChartPanel();
          clearDetailPanel();

          view.getHistoryConfigChartDetail().revalidate();
          view.getHistoryConfigChartDetail().repaint();
          view.getHistoryChartDetailSplitPane().revalidate();
          view.getHistoryChartDetailSplitPane().repaint();

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }
          chart.initialize();

          Boolean showLegend = UIState.INSTANCE.getShowLegend(model.getChartKey(), component.name(), ChartLegendState.SHOW);
          handleLegendChangeAll(showLegend);

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
            addChartToPanel(chart);
            addDetailToPanel(detail);

            boolean isCustom = chart.getSeriesType() == SeriesType.CUSTOM;
            view.getHistoryFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> log.error("Error creating/updating history chart", e),
        () -> createProgressBar("Creating/updating history chart..."),
        () -> log.info("Creating/updating history chart complete")
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

    GroupFunction groupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
    if (groupFunction != null) {
      metricCopy.setGroupFunction(groupFunction);
      metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    RangeHistory local = UIState.INSTANCE.getHistoryRange(chartKey);
    RangeHistory global = UIState.INSTANCE.getHistoryRangeAll(component.name());
    chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(local,
                                                             Objects.requireNonNullElse(global, RangeHistory.DAY)));

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

    config.setChartKey(chartKey);
    config.setTitle("");
    config.setXAxisLabel(model.getMetric().getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metricCopy);
    config.setChartInfo(chartInfoCopy);
    config.setQueryInfo(model.getQueryInfo());

    return config;
  }

  public void handleGroupFunctionChange(String action, GroupFunction function) {
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putHistoryGroupFunction(model.getChartKey(), function);

    view.getHistoryFilterPanel().clearFilterPanel();
    updateChart();
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);
    view.getHistoryRangePanel().setSelectedRange(range);
    updateChart();
  }

  private void handleTimeRangeFunctionChange(String action, TimeRangeFunction function) {
    UIState.INSTANCE.putTimeRangeFunction(model.getChartKey(), function);
    metric.setTimeRangeFunction(function);
    updateChart();
  }

  private void handleNormFunctionChange(String action, NormFunction function) {
    UIState.INSTANCE.putNormFunction(model.getChartKey(), function);
    metric.setNormFunction(function);
    updateChart();
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