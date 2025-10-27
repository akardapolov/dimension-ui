package ru.dimension.ui.component.module.chart.main;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.api.ModuleView;
import ru.dimension.ui.component.module.chart.main.unit.HistoryUnitPresenter;
import ru.dimension.ui.component.module.chart.main.unit.HistoryUnitView;
import ru.dimension.ui.component.module.chart.main.unit.InsightUnitPresenter;
import ru.dimension.ui.component.module.chart.main.unit.InsightUnitView;
import ru.dimension.ui.component.module.chart.main.unit.RealtimeUnitPresenter;
import ru.dimension.ui.component.module.chart.main.unit.RealtimeUnitView;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.UIState;

@Log4j2
public class ChartPresenter implements MessageAction {

  private final MessageBroker.Component component;
  @Getter
  private final ChartModel model;

  private final ModuleView moduleView;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final RealtimeUnitView realtimeUnitView;
  private final HistoryUnitView historyUnitView;
  private final InsightUnitView insightUnitView;

  private final RealtimeUnitPresenter realtimeUnit;
  private final HistoryUnitPresenter historyUnit;
  private final InsightUnitPresenter insightUnit;
  public ChartPresenter(MessageBroker.Component component,
                        ChartModel model,
                        ModuleView moduleView,
                        RealtimeUnitView realtimeUnitView,
                        HistoryUnitView historyUnitView,
                        InsightUnitView insightUnitView) {
    this.component = component;
    this.model = model;
    this.moduleView = moduleView;
    this.realtimeUnitView = realtimeUnitView;
    this.historyUnitView = historyUnitView;
    this.insightUnitView = insightUnitView;

    this.realtimeUnit = new RealtimeUnitPresenter(component, model, this.realtimeUnitView, executor);
    this.historyUnit = new HistoryUnitPresenter(component, model, this.historyUnitView, executor);
    this.insightUnit = new InsightUnitPresenter(component, model, this.insightUnitView, executor);

    this.realtimeUnit.initializePresenter();
  }

  public void initializeCharts() {
    realtimeUnit.initializeCharts();
  }

  public void initHistoryUnitIfNeeded() {
    historyUnit.initializeIfNeeded();
  }

  public void initInsightUnitIfNeeded() {
    insightUnit.initializeIfNeeded();
  }

  public void handleLegendChangeAll(ChartLegendState chartLegendState) {
    boolean show = ChartLegendState.SHOW.equals(chartLegendState);
    handleLegendChangeAll(show);
  }

  public void handleLegendChangeAll(Boolean showLegend) {
    realtimeUnit.handleLegendChangeAll(showLegend);
    historyUnit.handleLegendChangeAll(showLegend);
    insightUnit.handleLegendChangeAll(showLegend);
  }

  public void setActiveTab(MessageBroker.Panel panel) {
    moduleView.getTabbedPane().setSelectedIndex(panel.ordinal());
  }

  public void updateRealTimeRange(RangeRealTime range) {
    realtimeUnitView.getRealTimeRangePanel().setSelectedRange(range);
    realtimeUnit.handleRealTimeRangeChange("configChange", range);
  }

  public void updateHistoryRange(RangeHistory range) {
    historyUnit.initializeIfNeeded();
    historyUnitView.getHistoryRangePanel().setSelectedRange(range);
    historyUnit.handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    historyUnit.initializeIfNeeded();
    UIState.INSTANCE.putHistoryCustomRange(model.getChartKey(), range);
    UIState.INSTANCE.putHistoryRange(model.getChartKey(), RangeHistory.CUSTOM);
    model.getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    historyUnitView.getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    historyUnitView.getHistoryRangePanel().getDateTimePickerFrom().setDate(new java.util.Date(range.getBegin()));
    historyUnitView.getHistoryRangePanel().getDateTimePickerTo().setDate(new java.util.Date(range.getEnd()));

    historyUnit.updateChart();
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
    Map<String, Color> seriesColorMap = message.parameters().get("seriesColorMap");

    switch (panel) {
      case REALTIME -> realtimeUnit.handleFilterChange(topMapSelected, seriesColorMap);
      case HISTORY -> {
        historyUnit.initializeIfNeeded();
        historyUnit.handleFilterChange(topMapSelected, seriesColorMap);
      }
      case INSIGHT -> {
        insightUnit.initializeIfNeeded();
        insightUnit.handleFilterChange(topMapSelected, seriesColorMap);
      }
      default -> log.warn("Unsupported panel {}", panel);
    }
  }
}