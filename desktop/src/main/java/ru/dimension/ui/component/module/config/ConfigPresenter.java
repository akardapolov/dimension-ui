package ru.dimension.ui.component.module.config;

import java.util.Date;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
public class ConfigPresenter {
  private final MessageBroker.Component component;

  private final ConfigView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ConfigPresenter(MessageBroker.Component component,
                         ConfigView view) {
    this.component = component;
    this.view = view;

    setupListeners();
  }

  private void setupListeners() {
    view.getSwitchToTabPanel().getRealTime().addActionListener(e -> handleTabChange(Panel.REALTIME));
    view.getSwitchToTabPanel().getHistory().addActionListener(e -> handleTabChange(Panel.HISTORY));

    view.getRealTimePanel().getFiveMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.FIVE_MIN));
    view.getRealTimePanel().getTenMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.TEN_MIN));
    view.getRealTimePanel().getThirtyMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.THIRTY_MIN));
    view.getRealTimePanel().getSixtyMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.SIXTY_MIN));

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

    UIState.INSTANCE.putShowLegendAll(component.name(), ChartLegendState.SHOW.equals(chartLegendState));

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(Action.CHART_LEGEND_STATE_ALL)
                           .parameter("chartLegendState", chartLegendState)
                           .build());
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    log.info("Set card state in {} to: {}", component.name(), cardState);

    UIState.INSTANCE.putChartCardStateAll(component.name(), cardState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(Action.EXPAND_COLLAPSE_ALL)
                           .parameter("cardState", cardState)
                           .build());
  }

  private void handleTabChange(MessageBroker.Panel panel) {
    log.info("Tab changed to: {}", panel);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(Action.CHANGE_TAB)
                           .parameter("panel", panel)
                           .build());
  }

  private void handleRealTimeRangeChange(RangeRealTime range) {
    log.info("Real-time range changed to: {}", range);

    UIState.INSTANCE.putRealTimeRangeAll(component.name(), range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(Action.REALTIME_RANGE_CHANGE)
                           .parameter("range", range)
                           .build());

    handleTabChange(Panel.REALTIME);
  }

  private void handleHistoryRangeChange(RangeHistory range) {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("History range changed to: {}", range);
    updateHistoryRange(range, chartRange);
    handleTabChange(Panel.HISTORY);
  }

  private void handleCustomHistoryRangeChange() {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("Custom history chart range changed to: {}", chartRange);
    updateHistoryRange(RangeHistory.CUSTOM, chartRange);
    handleTabChange(Panel.HISTORY);

    view.getHistoryPanel().setSelectedRange(RangeHistory.CUSTOM);
  }

  private ChartRange getChartRangeFromPickers() {
    DateTimePicker fromPicker = view.getHistoryPanel().getDateTimePickerFrom();
    DateTimePicker toPicker = view.getHistoryPanel().getDateTimePickerTo();

    Date fromDate = fromPicker.getDate();
    Date toDate = toPicker.getDate();

    return new ChartRange(fromDate.getTime(), toDate.getTime());
  }

  private void updateHistoryRange(RangeHistory range,
                                  ChartRange chartRange) {
    String componentName = component.name();
    UIState.INSTANCE.putHistoryCustomRangeAll(componentName, chartRange);
    UIState.INSTANCE.putHistoryRangeAll(componentName, range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(Action.HISTORY_RANGE_CHANGE)
                           .parameter("range", range)
                           .parameter("chartRange", chartRange)
                           .build());
  }
}