package ru.dimension.ui.component.module.chart;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.chart.unit.HistoryUnitPresenter;
import ru.dimension.ui.component.module.chart.unit.RealtimeUnitPresenter;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.UIState;

@Log4j2
public class ChartPresenter implements MessageAction {

  private final MessageBroker.Component component;
  private final ChartModel model;
  private final ChartView view;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final RealtimeUnitPresenter realtimeUnit;
  private final HistoryUnitPresenter historyUnit;

  public ChartPresenter(MessageBroker.Component component,
                        ChartModel model,
                        ChartView view) {
    this.component = component;
    this.model = model;
    this.view = view;

    this.realtimeUnit = new RealtimeUnitPresenter(component, model, view.getRealtimeUnitView(), executor);
    this.historyUnit = new HistoryUnitPresenter(component, model, view.getHistoryUnitView(), executor);

    this.realtimeUnit.initializePresenter();
    this.historyUnit.initializePresenter();
  }

  public void initializeCharts() {
    realtimeUnit.initializeCharts();
    historyUnit.initializeCharts();
  }

  public void handleLegendChangeAll(ChartLegendState chartLegendState) {
    boolean show = ChartLegendState.SHOW.equals(chartLegendState);
    handleLegendChangeAll(show);
  }

  public void handleLegendChangeAll(Boolean showLegend) {
    realtimeUnit.handleLegendChangeAll(showLegend);
    historyUnit.handleLegendChangeAll(showLegend);
  }

  public void setActiveTab(MessageBroker.Panel panel) {
    view.getTabbedPane().setSelectedIndex(panel.ordinal());
  }

  public void updateRealTimeRange(RangeRealTime range) {
    view.getRealtimeUnitView().getRealTimeRangePanel().setSelectedRange(range);
    realtimeUnit.handleRealTimeRangeChange("configChange", range);
  }

  public void updateHistoryRange(RangeHistory range) {
    view.getHistoryUnitView().getHistoryRangePanel().setSelectedRange(range);
    historyUnit.handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    UIState.INSTANCE.putHistoryCustomRange(model.getChartKey(), range);
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), RangeHistory.CUSTOM);
    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    view.getHistoryUnitView().getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    view.getHistoryUnitView().getHistoryRangePanel().getDateTimePickerFrom().setDate(new java.util.Date(range.getBegin()));
    view.getHistoryUnitView().getHistoryRangePanel().getDateTimePickerTo().setDate(new java.util.Date(range.getEnd()));

    historyUnit.updateHistoryChart();
  }

  public SCP getRealTimeChart() {
    return realtimeUnit.getChart();
  }

  public boolean isReadyRealTimeUpdate() {
    return realtimeUnit.isReadyUpdate();
  }

  @Override
  public void receive(Message message) {
    log.info("Message received >>> {} with action >>> {}", message.destination(), message.action());

    Panel panel = message.destination().panel();
    Map<CProfile, LinkedHashSet<String>> topMapSelected = message.parameters().get("topMapSelected");
    Map<String, java.awt.Color> seriesColorMap = message.parameters().get("seriesColorMap");

    switch (panel) {
      case REALTIME -> realtimeUnit.handleFilterChange(topMapSelected, seriesColorMap);
      case HISTORY -> historyUnit.handleFilterChange(topMapSelected, seriesColorMap);
      default -> log.warn("Unsupported panel {}", panel);
    }
  }
}