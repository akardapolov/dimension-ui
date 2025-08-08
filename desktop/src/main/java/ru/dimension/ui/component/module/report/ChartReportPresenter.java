package ru.dimension.ui.component.module.report;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.chart.ChartModel;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public class ChartReportPresenter implements HelperChart {

  private final ChartModel model;
  private final ChartReportView view;

  private SCP historyChart;
  private DetailDashboardPanel historyDetail;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartReportPresenter(ChartModel model,
                              ChartReportView view) {
    this.model = model;
    this.view = view;
    initializePresenter();
  }

  private void initializePresenter() {
    initializeFromState();
    view.getHistoryMetricFunctionPanel().setRunAction(this::handleHistoryMetricFunctionChange);
    view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
    view.getHistoryLegendPanel().setStateChangeConsumer(
        showLegend -> handleLegendChange(ChartLegendState.SHOW.equals(showLegend))
    );
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

          historyChart = createChart();
          historyChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          historyDetail = getDetail(historyChart);

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);
          };
        },
        e -> log.error("Error creating history chart", e),
        () -> createProgressBar("Creating history chart..."),
        () -> setDetailState(UIState.INSTANCE.getShowDetailAll(Component.REPORT.name()))
    );
  }

  private SCP createChart() {
    ChartConfig config = buildChartConfig();
    DStore fStore = model.getDStore();
    ProfileTaskQueryKey key = model.getKey();
    ChartRange chartRange = getChartRange(config.getChartInfo());

    config.getChartInfo().setCustomBegin(chartRange.getBegin());
    config.getChartInfo().setCustomEnd(chartRange.getEnd());

    return new HistorySCP(fStore, config, key, null);
  }

  protected DetailDashboardPanel getDetail(SCP chart) {
    DetailDashboardPanel detailPanel = new DetailDashboardPanel(
        model.getDStore(),
        model.getQueryInfo(),
        model.getTableInfo(),
        chart.getConfig().getMetric(),
        chart.getSeriesColorMap(),
        ProcessType.HISTORY,
        chart.getSeriesType(),
        null
    );
    chart.addChartListenerReleaseMouse(detailPanel);
    return detailPanel;
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    ChartKey chartKey = new ChartKey(model.getKey(), model.getMetric().getYAxis());
    Metric metricCopy = model.getMetric().copy();
    ChartInfo chartInfoCopy = model.getChartInfo().copy();

    MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(chartKey);
    if (metricFunction != null) {
      metricCopy.setMetricFunction(metricFunction);
      metricCopy.setChartType(MetricFunction.COUNT.equals(metricFunction) ?
                                  ChartType.STACKED : ChartType.LINEAR);
    }

    RangeHistory range = UIState.INSTANCE.getHistoryRange(chartKey);
    chartInfoCopy.setRangeHistory(Objects.requireNonNullElse(range, RangeHistory.DAY));

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
    // History metric function initialization
    MetricFunction metricFunction = UIState.INSTANCE.getHistoryMetricFunction(chartKey);
    if (metricFunction == null) {
      view.getHistoryMetricFunctionPanel().setSelected(model.getMetric().getMetricFunction());
    } else {
      view.getHistoryMetricFunctionPanel().setSelected(metricFunction);
    }

    // History range initialization
    RangeHistory range = UIState.INSTANCE.getHistoryRange(chartKey);
    if (range != null) {
      view.getHistoryRangePanel().setSelectedRange(range);
    }

    // Legend visibility initialization
    Boolean showLegend = UIState.INSTANCE.getShowLegend(chartKey);
    if (showLegend != null) {
      view.getHistoryLegendPanel().setSelected(showLegend);
    }
  }

  private void handleHistoryMetricFunctionChange(String action,
                                                 MetricFunction function) {
    UIState.INSTANCE.putHistoryMetricFunction(model.getChartKey(), function);
    updateHistoryChart();
  }

  private void handleHistoryRangeChange(String action,
                                        RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), range);
    model.getChartInfo().setRangeHistory(range);
    updateHistoryChart();
  }

  private void updateHistoryChart() {
    SwingTaskRunner.runWithProgress(
        view.getHistoryChartPanel(),
        executor,
        () -> {
          view.getHistoryChartPanel().removeAll();
          view.getHistoryDetailPanel().removeAll();

          historyChart = null;
          historyDetail = null;

          historyChart = createChart();
          historyChart.initialize();

          handleLegendChange(UIState.INSTANCE.getShowLegend(model.getChartKey()));

          historyDetail = getDetail(historyChart);

          return () -> {
            view.getHistoryChartPanel().add(historyChart, BorderLayout.CENTER);
            view.getHistoryDetailPanel().add(historyDetail, BorderLayout.CENTER);
          };
        },
        e -> log.error("Error updating history chart", e),
        () -> createProgressBar("Updating history chart..."),
        () -> setDetailState(UIState.INSTANCE.getShowDetailAll(Component.REPORT.name()))
    );
  }

  private void handleLegendChange(Boolean showLegend) {
    boolean visibility = showLegend != null ? showLegend : true;
    updateLegendVisibility(visibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), visibility);
  }

  private void updateLegendVisibility(boolean visibility) {
    if (historyChart != null) {
      view.getHistoryLegendPanel().setSelected(visibility);
      historyChart.getjFreeChart().getLegend().setVisible(visibility);
      historyChart.repaint();
    }
  }

  private void setDetailState(DetailState detailState) {
    view.setDetailVisible(detailState == DetailState.SHOW);
    if (historyChart != null) {
      historyChart.clearSelectionRegion();
    }
  }
}
