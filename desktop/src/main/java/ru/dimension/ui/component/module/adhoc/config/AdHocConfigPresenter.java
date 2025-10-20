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
      log.info("Global key is empty");
    } else {
      adHocStateManager.putGlobalShowLegend(model.getGlobalKey(), ChartLegendState.SHOW.equals(chartLegendState));
      broker.sendMessage(Message.builder()
                             .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                             .action(Action.CHART_LEGEND_STATE_ALL)
                             .parameter("globalKey", model.getGlobalKey())
                             .parameter("chartLegendState", chartLegendState)
                             .build());
    }
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    log.info("Set card state in ad-hoc to: {}", cardState);

    if (model.getGlobalKey().isEmpty()) {
      log.info("Global key is empty");
    } else {
      adHocStateManager.putGlobalChartCardState(model.getGlobalKey(), cardState);
      broker.sendMessage(Message.builder()
                             .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                             .action(Action.EXPAND_COLLAPSE_ALL)
                             .parameter("globalKey", model.getGlobalKey())
                             .parameter("cardState", cardState)
                             .build());
    }
  }

  private void handleHistoryRangeChange(RangeHistory range) {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("History range changed to: {}", range);
    updateHistoryRange(range, chartRange);
  }

  private void handleCustomHistoryRangeChange() {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("Custom history chart range changed to: {}", chartRange);
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
      log.info("Global key is empty");
    } else {
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
    model.setGlobalKey(globalKey);

    ChartRange chartRange = adHocStateManager.getCustomChartRange(new AdHocKey(), globalKey);
    view.getHistoryPanel().getDateTimePickerFrom().setDate(new Date(chartRange.getBegin()));
    view.getHistoryPanel().getDateTimePickerTo().setDate(new Date(chartRange.getEnd()));

    RangeHistory rangeHistory = adHocStateManager.getHistoryRange(new AdHocKey(), globalKey);
    view.getHistoryPanel().setSelectedRange(rangeHistory);

    boolean showLegend = adHocStateManager.getShowLegend(new AdHocKey(), globalKey);
    view.getLegendPanel().setSelected(showLegend);

    ChartCardState chartCardState = adHocStateManager.getChartCardStateAll(globalKey);
    view.getCollapseCardPanel().setState(chartCardState);
  }
}