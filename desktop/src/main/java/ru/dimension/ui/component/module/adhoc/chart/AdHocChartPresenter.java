package ru.dimension.ui.component.module.adhoc.chart;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.module.adhoc.chart.unit.AdHocHistoryUnitPresenter;
import ru.dimension.ui.component.module.adhoc.chart.unit.AdHocHistoryUnitView;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.AdHocStateManager;

@Log4j2
public class AdHocChartPresenter implements MessageAction {

  private final AdHocHistoryUnitPresenter historyUnit;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public AdHocChartPresenter(AdHocChartModel model, AdHocHistoryUnitView historyUnitView) {
    this.historyUnit = new AdHocHistoryUnitPresenter(model, historyUnitView, executor);
    this.historyUnit.initializePresenter();
  }

  public void initializeChart() {
    historyUnit.initializeCharts();
  }

  public void handleLegendChange(Boolean showLegend) {
    historyUnit.handleLegendChangeAll(showLegend);
  }

  public void updateHistoryRange(RangeHistory range) {
    historyUnit.getView().getHistoryRangePanel().setSelectedRange(range);
    historyUnit.handleHistoryRangeChange("configChange", range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    AdHocStateManager.getInstance().putHistoryRange(historyUnit.getModel().getAdHocKey(), RangeHistory.CUSTOM);
    AdHocStateManager.getInstance().putHistoryCustomRange(historyUnit.getModel().getAdHocKey(), range);
    historyUnit.getModel().getChartInfo().setRangeHistory(RangeHistory.CUSTOM);

    historyUnit.getView().getHistoryRangePanel().setSelectedRange(RangeHistory.CUSTOM);
    historyUnit.getView().getHistoryRangePanel().getDateTimePickerFrom().setDate(new java.util.Date(range.getBegin()));
    historyUnit.getView().getHistoryRangePanel().getDateTimePickerTo().setDate(new java.util.Date(range.getEnd()));
    historyUnit.updateChart();
  }

  @Override
  public void receive(Message message) {
    log.info("Message received >>> {} with action >>> {}", message.destination(), message.action());

    Map<CProfile, LinkedHashSet<String>> topMapSelected = message.parameters().get("topMapSelected");
    Map<String, Color> seriesColorMap = message.parameters().get("seriesColorMap");

    historyUnit.getModel().updateMaps(topMapSelected, seriesColorMap);
    historyUnit.handleFilterChange(topMapSelected, seriesColorMap);
  }
}