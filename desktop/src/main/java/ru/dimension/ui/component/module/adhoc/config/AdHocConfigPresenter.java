package ru.dimension.ui.component.module.adhoc.config;

import java.util.Date;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.AdHocStateManager;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
public class AdHocConfigPresenter implements MessageAction {

  private final AdHocConfigModel model;
  private final AdHocConfigView view;

  private final MessageBroker broker = MessageBroker.getInstance();
  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  public AdHocConfigPresenter(AdHocConfigModel model,
                              AdHocConfigView view) {
    this.model = model;
    this.view = view;

    setupListeners();

    model.setGlobalKey("");
    resetConfigUIToDefaults();
    setConfigControlsEnabled(false);
  }

  private void setupListeners() {
    view.getHistoryPanel().getDay().addActionListener(e -> handleHistoryRangeChange(RangeHistory.DAY));
    view.getHistoryPanel().getWeek().addActionListener(e -> handleHistoryRangeChange(RangeHistory.WEEK));
    view.getHistoryPanel().getMonth().addActionListener(e -> handleHistoryRangeChange(RangeHistory.MONTH));
    view.getHistoryPanel().getCustom().addActionListener(e -> handleHistoryRangeChange(RangeHistory.CUSTOM));
    view.getHistoryPanel().getButtonApplyRange().addActionListener(e -> handleCustomHistoryRangeChange());

    view.getLegendPanel().setStateChangeConsumer(this::handleLegendVisibilityChange);
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleLegendVisibilityChange(ChartLegendState chartLegendState) {
    log.info("Legend visibility changed to: {}", chartLegendState);

    if (model.getGlobalKey().isEmpty()) {
      log.debug("Skip legend change: globalKey is empty (no active ad-hoc tab)");
      return;
    }

    adHocStateManager.putGlobalShowLegend(model.getGlobalKey(), ChartLegendState.SHOW.equals(chartLegendState));

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.CHART_LEGEND_STATE_ALL)
                           .parameter("globalKey", model.getGlobalKey())
                           .parameter("chartLegendState", chartLegendState)
                           .build());
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    log.info("Set card state in ad-hoc to: {}", cardState);

    if (model.getGlobalKey().isEmpty()) {
      log.debug("Skip collapse/expand: globalKey is empty (no active ad-hoc tab)");
      return;
    }

    adHocStateManager.putGlobalChartCardState(model.getGlobalKey(), cardState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.EXPAND_COLLAPSE_ALL)
                           .parameter("globalKey", model.getGlobalKey())
                           .parameter("cardState", cardState)
                           .build());
  }

  private void handleHistoryRangeChange(RangeHistory range) {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("History range changed to: {}", range);

    if (model.getGlobalKey().isEmpty()) {
      log.debug("Skip history range change: globalKey is empty (no active ad-hoc tab)");
      return;
    }

    updateHistoryRange(range, chartRange);
  }

  private void handleCustomHistoryRangeChange() {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("Custom history chart range changed to: {}", chartRange);

    if (model.getGlobalKey().isEmpty()) {
      log.debug("Skip custom range apply: globalKey is empty (no active ad-hoc tab)");
      return;
    }

    updateHistoryRange(RangeHistory.CUSTOM, chartRange);
    view.getHistoryPanel().setSelectedRange(RangeHistory.CUSTOM);
  }

  private ChartRange getChartRangeFromPickers() {
    DateTimePicker fromPicker = view.getHistoryPanel().getDateTimePickerFrom();
    DateTimePicker toPicker = view.getHistoryPanel().getDateTimePickerTo();

    Date fromDate = fromPicker.getDate();
    Date toDate = toPicker.getDate();

    return new ChartRange(fromDate.getTime(), toDate.getTime());
  }

  private void updateHistoryRange(RangeHistory range, ChartRange chartRange) {
    if (model.getGlobalKey().isEmpty()) {
      log.debug("Skip updateHistoryRange: globalKey is empty (no active ad-hoc tab)");
      return;
    }

    adHocStateManager.putGlobalHistoryCustomRange(model.getGlobalKey(), chartRange);
    adHocStateManager.putGlobalHistoryRange(model.getGlobalKey(), range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.HISTORY_RANGE_CHANGE)
                           .parameter("globalKey", model.getGlobalKey())
                           .parameter("range", range)
                           .parameter("chartRange", chartRange)
                           .build());
  }

  @Override
  public void receive(Message message) {
    switch (message.action()) {
      case HISTORY_CUSTOM_UI_RANGE_CHANGE -> handleCustomUIHistoryRangeChange(message);
    }
  }

  private void handleCustomUIHistoryRangeChange(Message message) {
    log.info("Message action {}", message.action());

    String globalKey = message.parameters().get("globalKey");

    if (globalKey == null || globalKey.isBlank()) {
      model.setGlobalKey("");
      resetConfigUIToDefaults();
      setConfigControlsEnabled(false);
      return;
    }

    setConfigControlsEnabled(true);
    model.setGlobalKey(globalKey);

    ChartRange chartRange = adHocStateManager.getCustomChartRange(new AdHocKey(), globalKey);
    if (chartRange == null) {
      long end = System.currentTimeMillis();
      chartRange = new ChartRange(end - 24L * 60L * 60L * 1000L, end);
      adHocStateManager.putGlobalHistoryCustomRange(globalKey, chartRange);
    }
    view.getHistoryPanel().getDateTimePickerFrom().setDate(new java.util.Date(chartRange.getBegin()));
    view.getHistoryPanel().getDateTimePickerTo().setDate(new java.util.Date(chartRange.getEnd()));

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(new AdHocKey(), globalKey);
    if (rangeHistory == null) {
      rangeHistory = RangeHistory.DAY;
      adHocStateManager.putGlobalHistoryRange(globalKey, rangeHistory);
    }
    view.getHistoryPanel().setSelectedRange(rangeHistory);

    Boolean showLegend = adHocStateManager.getShowLegend(new AdHocKey(), globalKey);
    if (showLegend == null) {
      showLegend = true;
      adHocStateManager.putGlobalShowLegend(globalKey, true);
    }
    view.getLegendPanel().setSelected(showLegend);

    ChartCardState chartCardState = adHocStateManager.getChartCardStateAll(globalKey);
    if (chartCardState == null) {
      chartCardState = ChartCardState.COLLAPSE_ALL;
      adHocStateManager.putGlobalChartCardState(globalKey, chartCardState);
    }
    view.getCollapseCardPanel().setState(chartCardState);
  }

  private void resetConfigUIToDefaults() {
    view.getHistoryPanel().setSelectedRange(RangeHistory.DAY);

    Date now = new Date();
    view.getHistoryPanel().getDateTimePickerTo().setDate(now);
    view.getHistoryPanel().getDateTimePickerFrom().setDate(new Date(now.getTime() - 24L * 60L * 60L * 1000L));

    view.getLegendPanel().setSelected(true);
    view.getCollapseCardPanel().setState(ChartCardState.COLLAPSE_ALL);
  }

  private void setPanelEnabledRecursively(java.awt.Component c, boolean enabled) {
    c.setEnabled(enabled);
    if (c instanceof java.awt.Container container) {
      for (java.awt.Component child : container.getComponents()) {
        setPanelEnabledRecursively(child, enabled);
      }
    }
  }

  private void setConfigControlsEnabled(boolean enabled) {
    setPanelEnabledRecursively(view.getHistoryPanel(), enabled);
    setPanelEnabledRecursively(view.getLegendPanel(), enabled);
    setPanelEnabledRecursively(view.getCollapseCardPanel(), enabled);
  }
}