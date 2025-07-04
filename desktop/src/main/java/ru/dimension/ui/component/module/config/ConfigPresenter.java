package ru.dimension.ui.component.module.config;

import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.Destination;
import ru.dimension.ui.component.Message;
import ru.dimension.ui.component.MessageBroker;
import ru.dimension.ui.component.MessageBroker.Action;
import ru.dimension.ui.component.MessageBroker.Component;
import ru.dimension.ui.component.MessageBroker.Module;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.model.DetailState;

@Log4j2
public class ConfigPresenter {

  private final ConfigView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ConfigPresenter(ConfigView view) {
    this.view = view;

    UIState.INSTANCE.putShowDetailAll(Component.DASHBOARD.name(), DetailState.SHOW);

    setupListeners();
  }

  private void setupListeners() {
    // Handle tab selection
    view.getSwitchToTabPanel().getRealTime().addActionListener(e -> handleTabChange(PanelTabType.REALTIME));
    view.getSwitchToTabPanel().getHistory().addActionListener(e -> handleTabChange(PanelTabType.HISTORY));

    // Handle real-time range changes
    view.getRealTimePanel().getFiveMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.FIVE_MIN));
    view.getRealTimePanel().getTenMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.TEN_MIN));
    view.getRealTimePanel().getThirtyMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.THIRTY_MIN));
    view.getRealTimePanel().getSixtyMin().addActionListener(e -> handleRealTimeRangeChange(RangeRealTime.SIXTY_MIN));

    // Handle history range changes
    view.getHistoryPanel().getDay().addActionListener(e -> handleHistoryRangeChange(RangeHistory.DAY));
    view.getHistoryPanel().getWeek().addActionListener(e -> handleHistoryRangeChange(RangeHistory.WEEK));
    view.getHistoryPanel().getMonth().addActionListener(e -> handleHistoryRangeChange(RangeHistory.MONTH));

    view.getLegendPanel().setVisibilityConsumer(this::handleLegendVisibilityChange);
    view.getDetailShowHidePanel().setStateChangeConsumer(this::handleDetailVisibilityChange);
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleLegendVisibilityChange(ChartLegendState chartLegendState) {
    log.info("Legend visibility changed to: {}", chartLegendState);

    UIState.INSTANCE.putShowLegendAll(Component.DASHBOARD.name(), ChartLegendState.SHOW.equals(chartLegendState));

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.CHART_LEGEND_STATE_ALL)
                           .parameter("chartLegendState", chartLegendState)
                           .build());
  }

  private void handleDetailVisibilityChange(DetailState detailState) {
    log.info("Detail visibility changed to: {}", detailState);

    UIState.INSTANCE.putShowDetailAll(Component.DASHBOARD.name(), detailState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.SHOW_HIDE_DETAIL_ALL)
                           .parameter("detailState", detailState)
                           .build());
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    log.info("Set card state in dashboard to: {}", cardState);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.EXPAND_COLLAPSE_ALL)
                           .parameter("cardState", cardState)
                           .build());
  }

  private void handleTabChange(PanelTabType panelTabType) {
    log.info("Tab changed to: {}", panelTabType);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.CHANGE_TAB)
                           .parameter("panelTabType", panelTabType)
                           .build());
  }

  private void handleRealTimeRangeChange(RangeRealTime range) {
    log.info("Real-time range changed to: {}", range);

    UIState.INSTANCE.putRealTimeRangeAll(Component.DASHBOARD.name(), range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.REALTIME_RANGE_CHANGE)
                           .parameter("range", range)
                           .build());

    handleTabChange(PanelTabType.REALTIME);
  }

  private void handleHistoryRangeChange(RangeHistory range) {
    log.info("History range changed to: {}", range);

    UIState.INSTANCE.putHistoryRangeAll(Component.DASHBOARD.name(), range);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(Component.DASHBOARD, Module.CHARTS))
                           .action(Action.HISTORY_RANGE_CHANGE)
                           .parameter("range", range)
                           .build());

    handleTabChange(PanelTabType.HISTORY);
  }
}