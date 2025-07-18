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
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.model.DetailState;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
public class AdHocConfigPresenter implements MessageAction {

  private final AdHocConfigView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public AdHocConfigPresenter(AdHocConfigView view) {
    this.view = view;

    UIState.INSTANCE.putShowDetailAll(Component.ADHOC.name(), DetailState.SHOW);

    setupListeners();
  }

  private void setupListeners() {
    view.getHistoryPanel().getDay().addActionListener(e -> handleHistoryRangeChange(RangeHistory.DAY));
    view.getHistoryPanel().getWeek().addActionListener(e -> handleHistoryRangeChange(RangeHistory.WEEK));
    view.getHistoryPanel().getMonth().addActionListener(e -> handleHistoryRangeChange(RangeHistory.MONTH));
    view.getHistoryPanel().getCustom().addActionListener(e -> handleHistoryRangeChange(RangeHistory.CUSTOM));
    view.getHistoryPanel().getButtonApplyRange().addActionListener(e -> handleCustomHistoryRangeChange());

    view.getLegendPanel().setVisibilityConsumer(this::handleLegendVisibilityChange);
    view.getDetailShowHidePanel().setStateChangeConsumer(this::handleDetailVisibilityChange);
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleLegendVisibilityChange(ChartLegendState chartLegendState) {
    log.info("Legend visibility changed to: {}", chartLegendState);

    UIState.INSTANCE.putShowLegendAll(Component.ADHOC.name(), ChartLegendState.SHOW.equals(chartLegendState));

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.CHART_LEGEND_STATE_ALL)
                           .parameter("chartLegendState", chartLegendState)
                           .build());
  }

  private void handleDetailVisibilityChange(DetailState detailState) {
    log.info("Detail visibility changed to: {}", detailState);

    UIState.INSTANCE.putShowDetailAll(Component.ADHOC.name(), detailState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.SHOW_HIDE_DETAIL_ALL)
                           .parameter("detailState", detailState)
                           .build());
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    log.info("Set card state in ad-hoc to: {}", cardState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.EXPAND_COLLAPSE_ALL)
                           .parameter("cardState", cardState)
                           .build());
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
    String componentName = Component.ADHOC.name();
    UIState.INSTANCE.putHistoryCustomRangeAll(componentName, chartRange);
    UIState.INSTANCE.putHistoryRangeAll(componentName, range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.ADHOC, Module.CHARTS))
                           .action(Action.HISTORY_RANGE_CHANGE)
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

    ChartRange chartRange = message.parameters().get("chartRange");
    view.getHistoryPanel().getDateTimePickerFrom().setDate(new Date(chartRange.getBegin()));
    view.getHistoryPanel().getDateTimePickerTo().setDate(new Date(chartRange.getEnd()));
  }
}