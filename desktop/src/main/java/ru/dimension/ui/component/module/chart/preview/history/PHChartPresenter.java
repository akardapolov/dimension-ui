package ru.dimension.ui.component.module.chart.preview.history;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.base.BaseUnitPresenter;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
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
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PHChartPresenter extends BaseUnitPresenter<PHChartView> implements HelperChart {

  private final DetailChartContext detailContext;

  public PHChartPresenter(MessageBroker.Component component,
                          PHChartModel model,
                          PHChartView view,
                          ExecutorService executor,
                          DetailChartContext detailContext) {
    super(component, model, view, executor);
    this.detailContext = detailContext;
  }

  @Override
  public void initializePresenter() {
    initializeFromState();
    setupHandlers();
    initializeFilterPanel();
  }

  @Override
  public void initializeCharts() {
    updateChartInternal(detailContext.seriesColorMap(), detailContext.topMapSelected());
  }

  @Override
  public void updateChart() {
    updateChartInternal(detailContext.seriesColorMap(), detailContext.topMapSelected());
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
        view.getChartPanel(),
        executor,
        () -> {
          clearChartPanel();

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null && !seriesColorMap.isEmpty()) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }

          chart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          ChartRange chartRange = getChartRange(chart.getConfig().getChartInfo());
          view.getHistoryFilterPanel()
              .setDataSource(model.getDStore(), metric, chartRange.getBegin(), chartRange.getEnd());

          if (seriesColorMap == null) {
            view.getHistoryFilterPanel().clearFilterPanel();
          }
          view.getHistoryFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getHistoryFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());

          return () -> {
            addChartToPanel(chart);
            view.getHistoryFilterPanel().setEnabled(false);
          };
        },
        e -> log.error("Error updating history preview chart", e),
        () -> createProgressBar("Updating history preview chart...")
    );
  }

  private void initializeFromState() {
    ChartKey chartKey = model.getChartKey();

    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getHistoryFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getHistoryFunctionPanel().setEnabled(true, true, true);
    }
    GroupFunction groupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
    if (groupFunction != null) {
      metric.setGroupFunction(groupFunction);
    }
    view.getHistoryFunctionPanel().setSelected(metric.getGroupFunction());

    RangeHistory modelRange = model.getChartInfo().getRangeHistory();
    view.getHistoryRangePanel().setSelectedRange(modelRange);

    if (RangeHistory.CUSTOM.equals(modelRange)) {
      ChartRange customRange = new ChartRange(model.getChartInfo().getCustomBegin(),
                                              model.getChartInfo().getCustomEnd());
      view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
      view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey,
                                                        component.name(),
                                                        ChartLegendState.SHOW);
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

  private void setupHandlers() {
    view.getHistoryFunctionPanel().setRunAction(this::handleGroupFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
    view.getHistoryTimeRangeFunctionPanel().setRunAction(this::handleTimeRangeFunctionChange);
    view.getHistoryNormFunctionPanel().setRunAction(this::handleNormFunctionChange);

    view.getHistoryLegendPanel().setStateChangeConsumer(
        show -> handleLegendChange(ChartLegendState.SHOW.equals(show))
    );

    view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> applyCustomRange());

    view.setDetailsButtonAction(e -> sendShowChartFullMessage());
  }

  private void initializeFilterPanel() {
    view.getHistoryFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.HISTORY);
  }

  private SCP createChartInstance(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();

    ChartRange chartRange = getChartRange(config.getChartInfo());
    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());

    ProfileTaskQueryKey key = model.getKey();
    DStore dStore = model.getDStore();

    return new HistorySCP(dStore, config, key, topMapSelected, true);
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

    RangeHistory localRange = UIState.INSTANCE.getHistoryRange(chartKey,
                                                               component.name(),
                                                               RangeHistory.DAY);
    RangeHistory modelRange = chartInfoCopy.getRangeHistory();
    RangeHistory effective = modelRange != null ? modelRange : localRange;
    chartInfoCopy.setRangeHistory(effective);

    if (RangeHistory.CUSTOM.equals(effective)) {
      ChartRange customRange = UIState.INSTANCE.getHistoryCustomRange(chartKey);
      if (RangeHistory.CUSTOM.equals(modelRange)) {
        customRange = new ChartRange(model.getChartInfo().getCustomBegin(),
                                     model.getChartInfo().getCustomEnd());
      }
      if (customRange != null) {
        chartInfoCopy.setCustomBegin(customRange.getBegin());
        chartInfoCopy.setCustomEnd(customRange.getEnd());
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

  public void handleGroupFunctionChange(String action,
                                        GroupFunction function) {
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putHistoryGroupFunction(model.getChartKey(), function);
    view.getHistoryFilterPanel().clearFilterPanel();
    updateChart();
  }

  public void handleHistoryRangeChangeUI(RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);
    view.getHistoryRangePanel().setSelectedRange(range);
  }

  public void handleHistoryRangeChange(String action,
                                       RangeHistory range) {
    handleHistoryRangeChangeUI(range);
    updateChart();
  }

  private void handleTimeRangeFunctionChange(String action,
                                             TimeRangeFunction function) {
    UIState.INSTANCE.putTimeRangeFunction(model.getChartKey(), function);
    metric.setTimeRangeFunction(function);
    updateChart();
  }

  private void handleNormFunctionChange(String action,
                                        NormFunction function) {
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

  public void handleHistoryCustomRange(ChartRange range) {
    UIState.INSTANCE.putHistoryCustomRange(model.getChartKey(), range);
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), RangeHistory.CUSTOM);
    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);
    model.getChartInfo().setCustomBegin(range.getBegin());
    model.getChartInfo().setCustomEnd(range.getEnd());

    view.getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new java.util.Date(range.getBegin()));
    view.getHistoryRangePanel().getDateTimePickerTo().setDate(new java.util.Date(range.getEnd()));

    updateChart();
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

  private void sendShowChartFullMessage() {
    MessageBroker broker = MessageBroker.getInstance();
    Message message = Message.builder()
        .destination(Destination.builder()
                         .component(component)
                         .module(MessageBroker.Module.CHARTS)
                         .panel(Panel.NONE)
                         .block(Block.NONE)
                         .build())
        .action(MessageBroker.Action.SHOW_CHART_FULL)
        .parameter("chartKey", model.getChartKey())
        .parameter("key", model.getKey())
        .parameter("metric", metric)
        .parameter("queryInfo", model.getQueryInfo())
        .parameter("chartInfo", model.getChartInfo())
        .parameter("tableInfo", model.getTableInfo())
        .build();

    log.info("Message send >>> {} with action >>> {}", message.destination(), message.action());
    broker.sendMessage(message);
  }

  public void handleChartConfigState(ChartConfigState chartConfigState) {
    boolean showConfig = ChartConfigState.SHOW.equals(chartConfigState);
    log.info("Setting history config visibility to: {} for chart {}", showConfig, model.getChartKey());

    if (chart != null) {
      chart.snapshotSelectionRegion();
    }

    view.setChartConfigState(showConfig);

    if (chart != null) {
      chart.restoreSelectionRegionAfterNextDraw();
    }
  }
}