package ru.dimension.ui.component.module.chart.main.unit;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.component.chart.realtime.RealtimeSCP;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.base.BaseUnitPresenter;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.RealtimeStateProvider;
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
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;

@Log4j2
public class RealtimeUnitPresenter extends BaseUnitPresenter<RealtimeUnitView> implements HelperChart {

  @Getter
  private boolean isReadyUpdate = false;

  public RealtimeUnitPresenter(MessageBroker.Component component,
                               ChartModel model,
                               RealtimeUnitView view,
                               ExecutorService executor) {
    super(component, model, view, executor);
  }

  @Override
  public void initializePresenter() {
    initializeFromState();
    setupHandlers();
    initializeFilterPanel();
  }

  private void setupHandlers() {
    view.getRealTimeFunctionPanel().setRunAction(this::handleGroupFunctionChange);
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);

    view.getRealTimeLegendPanel()
        .setStateChangeConsumer(showLegend -> handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));
  }

  private void initializeFilterPanel() {
    view.getRealTimeFilterPanel().initializeChartPanel(model.getChartKey(), model.getTableInfo(), Panel.REALTIME);

    view.getRealTimeFilterPanel().setRealtimeStateProvider(new RealtimeStateProvider() {
      @Override
      public long provideCurrentBegin() {
        ChartKey chartKey = model.getChartKey();
        long end = model.getSqlQueryState().getLastTimestamp(chartKey.getProfileTaskQueryKey());
        if (end == 0L) {
          end = DateHelper.getNowMilli(ZoneId.systemDefault());
        }
        return end - getRangeRealTime(model.getChartInfo());
      }

      @Override
      public long provideCurrentEnd() {
        ChartKey chartKey = model.getChartKey();
        long end = model.getSqlQueryState().getLastTimestamp(chartKey.getProfileTaskQueryKey());
        if (end == 0L) {
          end = DateHelper.getNowMilli(ZoneId.systemDefault());
        }
        return end;
      }

      @Override
      public Map<String, Color> provideCurrentSeriesColorMap() {
        if (chart != null) {
          return chart.getSeriesColorMap();
        }
        return new HashMap<>();
      }
    });
  }

  private void initializeFromState() {
    ChartKey chartKey = model.getChartKey();

    GroupFunction groupFunction = UIState.INSTANCE.getRealtimeGroupFunction(chartKey);
    if (groupFunction != null) {
      metric.setGroupFunction(groupFunction);
      metric.setChartType(GroupFunction.COUNT.equals(groupFunction) ? ChartType.STACKED : ChartType.LINEAR);
    }

    if (CType.STRING.equals(model.getMetric().getYAxis().getCsType().getCType())) {
      view.getRealTimeFunctionPanel().setEnabled(true, false, false);
    } else {
      view.getRealTimeFunctionPanel().setEnabled(true, true, true);
    }
    view.getRealTimeFunctionPanel().setSelected(metric.getGroupFunction());

    RangeRealTime localRange = UIState.INSTANCE.getRealTimeRange(chartKey);
    RangeRealTime globalRange = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    RangeRealTime effective = localRange != null ? localRange : globalRange;

    if (effective != null) {
      view.getRealTimeRangePanel().setSelectedRange(effective);
    }

    Boolean chartLegendState = UIState.INSTANCE.getShowLegend(chartKey,
                                                              component.name(),
                                                              ChartLegendState.SHOW);
    view.getRealTimeLegendPanel().setSelected(chartLegendState);
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
        view.getRealTimeChartPanel(),
        executor,
        () -> {
          isReadyUpdate = false;
          clearChartPanel();
          clearDetailPanel();

          view.getRealTimeConfigChartDetail().revalidate();
          view.getRealTimeConfigChartDetail().repaint();
          view.getRealTimeChartDetailSplitPane().revalidate();
          view.getRealTimeChartDetailSplitPane().repaint();

          chart = createChartInstance(topMapSelected);

          if (seriesColorMap != null) {
            chart.loadSeriesColor(metric, seriesColorMap);
          }

          try {
            chart.initialize();
          } catch (SeriesExceedException e) {
            log.info("Series count exceeded threshold, reinitializing chart in custom mode for {}", model.getMetric()
                .getYAxis());
            if (chart instanceof ClientRealtimeSCP clientRealtimeSCP) {
              clientRealtimeSCP.reinitializeChartInCustomMode();
            } else if (chart instanceof ServerRealtimeSCP serverRealtimeSCP) {
              serverRealtimeSCP.reinitializeChartInCustomMode();
            }
            chart.initialize();
          }

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
              RealtimeSCP realtimeSCP = (RealtimeSCP) chart;
              detail = buildDetail(chart, chart.getSeriesColorMap(), SeriesType.CUSTOM, realtimeSCP.getFilter());
            }
          }

          ChartKey chartKey = model.getChartKey();
          long end = model.getSqlQueryState().getLastTimestamp(chartKey.getProfileTaskQueryKey());
          if (end == 0L) {
            end = DateHelper.getNowMilli(ZoneId.systemDefault());
          }
          long begin = end - getRangeRealTime(model.getChartInfo());
          view.getRealTimeFilterPanel().setDataSource(model.getDStore(), metric, begin, end);

          view.getRealTimeFilterPanel().setSeriesColorMap(chart.getSeriesColorMap());
          view.getRealTimeFilterPanel().getMetric().setGroupFunction(chart.getConfig().getMetric().getGroupFunction());

          return () -> {
            addChartToPanel(chart);
            addDetailToPanel(detail);
            isReadyUpdate = true;
            boolean isCustom = chart.getSeriesType() == SeriesType.CUSTOM;
            view.getRealTimeFilterPanel().setEnabled(!isCustom);
          };
        },
        e -> {
          isReadyUpdate = false;
          log.error("Error creating/updating real-time chart", e);
        },
        () -> createProgressBar("Creating/updating real-time chart..."),
        () -> {
          isReadyUpdate = true;
          log.info("Creating/updating real-time chart complete");
        }
    );
  }

  private SCP createChartInstance(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    ChartConfig config = buildChartConfig();
    ProfileTaskQueryKey key = model.getKey();
    ru.dimension.ui.state.SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();
    QueryInfo queryInfo = model.getQueryInfo();

    if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
      return new ClientRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
    } else {
      return new ServerRealtimeSCP(sqlQueryState, dStore, config, key, topMapSelected);
    }
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
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

  public void handleGroupFunctionChange(String action,
                                        ru.dimension.ui.model.function.GroupFunction function) {
    metric.setGroupFunction(function);
    metric.setChartType(GroupFunction.COUNT.equals(function) ? ChartType.STACKED : ChartType.LINEAR);
    UIState.INSTANCE.putRealtimeGroupFunction(model.getChartKey(), function);

    view.getRealTimeFilterPanel().clearFilterPanel();
    updateChart();
  }

  public void handleRealTimeRangeChange(String action,
                                        RangeRealTime range) {
    UIState.INSTANCE.putRealTimeRange(model.getChartKey(), range);
    model.getChartInfo().setRangeRealtime(range);
    updateChart();
  }
}