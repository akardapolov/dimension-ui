package ru.dimension.ui.component.module.chart.preview.realtime;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.base.BaseUnitPresenter;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PRChartPresenter extends BaseUnitPresenter<PRChartView> implements HelperChart {

  @Getter
  private boolean isReadyRealTimeUpdate = false;

  public PRChartPresenter(MessageBroker.Component component,
                          PRChartModel model,
                          PRChartView view,
                          ExecutorService executor) {
    super(component, model, view, executor);
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
  protected LegendPanel getLegendPanel() {
    return view.getRealTimeLegendPanel();
  }

  @Override
  protected Panel getPanelType() {
    return Panel.REALTIME;
  }

  @Override
  protected void updateChartInternal(Map<String, Color> seriesColorMap,
                                     Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    SwingTaskRunner.runWithProgress(
        view.getChartPanel(),
        executor,
        () -> {
          isReadyRealTimeUpdate = false;
          clearChartPanel();

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }

          try {
            chart.initialize();
          } catch (SeriesExceedException e) {
            log.info("Series count exceeded threshold, reinitializing chart in custom mode for {}", metric.getYAxis());
            if (chart instanceof ClientRealtimeSCP clientRealtimeSCP) {
              clientRealtimeSCP.reinitializeChartInCustomMode();
            } else if (chart instanceof ServerRealtimeSCP serverRealtimeSCP) {
              serverRealtimeSCP.reinitializeChartInCustomMode();
            }
            chart.initialize(); // Re-initialize after setting custom mode
          }

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          long end = model.getSqlQueryState().getLastTimestamp(model.getChartKey().getProfileTaskQueryKey());
          if (end == 0L) {
            end = DateHelper.getNowMilli(ZoneId.systemDefault());
          }
          long begin = end - getRangeRealTime(model.getChartInfo());
          view.getRealTimeFilterPanel().setDataSource(model.getDStore(), metric, begin, end);

          if (seriesColorMap == null) {
            view.getRealTimeFilterPanel().clearFilterPanel();
          }
          view.getRealTimeFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getRealTimeFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());

          return () -> {
            addChartToPanel(chart);
            isReadyRealTimeUpdate = true;
            view.getRealTimeFilterPanel().setEnabled(false);
          };
        },
        e -> {
          isReadyRealTimeUpdate = false;
          log.error("Error updating preview chart", e);
        },
        () -> createProgressBar("Updating preview chart...")
    );
  }

  private void initializeFromState() {
    ChartKey chartKey = model.getChartKey();

    GroupFunction groupFunction = UIState.INSTANCE.getRealtimeGroupFunction(chartKey);
    if (groupFunction != null) {
      metric.setGroupFunction(groupFunction);
      metric.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }
    view.getRealTimeFunctionPanel().setSelected(metric.getGroupFunction());

    RangeRealTime realTimeRange = UIState.INSTANCE.getRealTimeRange(chartKey,
                                                                    component.name(),
                                                                    RangeRealTime.TEN_MIN);
    view.getRealTimeRangePanel().setSelectedRange(realTimeRange);

    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey,
                                                        component.name(),
                                                        ChartLegendState.SHOW);
    view.getRealTimeLegendPanel().setSelected(showLegend);
  }

  private void setupHandlers() {
    view.getRealTimeFunctionPanel().setRunAction(this::handleRealtimeGroupFunctionChange);
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);
    view.getRealTimeLegendPanel().setStateChangeConsumer(
        show -> handleLegendChange(ChartLegendState.SHOW.equals(show))
    );
    view.setDetailsButtonAction(e -> sendShowChartFullMessage());
  }

  private void initializeFilterPanel() {
    view.getRealTimeFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.REALTIME);
  }

  private SCP createChartInstance(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();
    ProfileTaskQueryKey key = model.getKey();
    DStore dStore = model.getDStore();
    QueryInfo queryInfo = model.getQueryInfo();

    if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
      return new ClientRealtimeSCP(model.getSqlQueryState(), dStore, config, key, topMapSelected);
    } else {
      return new ServerRealtimeSCP(model.getSqlQueryState(), dStore, config, key, topMapSelected);
    }
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    ChartKey chartKey = model.getChartKey();

    Metric metricCopy = metric.copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();

    GroupFunction groupFunction = UIState.INSTANCE.getRealtimeGroupFunction(chartKey);
    if (groupFunction != null) {
      metricCopy.setGroupFunction(groupFunction);
      metricCopy.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

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

  private void handleRealtimeGroupFunctionChange(String action, GroupFunction function) {
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putRealtimeGroupFunction(model.getChartKey(), function);
    view.getRealTimeFilterPanel().clearFilterPanel();
    updateChart();
  }

  public void handleRealTimeRangeChangeUI(RangeRealTime range) {
    UIState.INSTANCE.putRealTimeRange(model.getChartKey(), range);
    model.getChartInfo().setRangeRealtime(range);
    view.getRealTimeRangePanel().setSelectedRange(range);
  }

  public void handleRealTimeRangeChange(String action, RangeRealTime range) {
    handleRealTimeRangeChangeUI(range);
    updateChart();
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
        .parameter("metric", metric) // Send the current metric with its function
        .parameter("queryInfo", model.getQueryInfo())
        .parameter("chartInfo", model.getChartInfo())
        .parameter("tableInfo", model.getTableInfo())
        .build();

    log.info("Message send >>> {} with action >>> {}", message.destination(), message.action());
    broker.sendMessage(message);
  }

  public void handleChartConfigState(ChartConfigState chartConfigState) {
    boolean showConfig = ChartConfigState.SHOW.equals(chartConfigState);
    log.info("Setting config chart visibility to: {} for chart {}", showConfig, model.getChartKey());

    if (chart != null) {
      chart.snapshotSelectionRegion();
    }

    view.setChartConfigState(showConfig);

    if (chart != null) {
      chart.restoreSelectionRegionAfterNextDraw();
    }
  }
}