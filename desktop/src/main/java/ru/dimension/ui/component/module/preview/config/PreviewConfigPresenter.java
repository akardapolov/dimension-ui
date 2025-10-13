package ru.dimension.ui.component.module.preview.config;

import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PreviewConfigPresenter {
  private static final RangeRealTime DEFAULT_RANGE = RangeRealTime.TEN_MIN;
  private static final boolean DEFAULT_SHOW_LEGEND = true;
  private static final boolean DEFAULT_SHOW_CONFIG = true;

  private final MessageBroker.Component component;

  private final PreviewConfigView  view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public PreviewConfigPresenter(MessageBroker.Component component,
                                PreviewConfigView view) {
    this.component = component;
    this.view  = view;
    restoreState();
    setupListeners();
  }

  private void restoreState() {
    RangeRealTime range = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    if (range == null) {
      range = DEFAULT_RANGE;
      UIState.INSTANCE.putRealTimeRangeAll(component.name(), range);
    }
    view.getRealTimeRangePanel().setSelectedRange(range);

    Boolean showLegend = UIState.INSTANCE.getShowLegendAll(component.name());
    if (showLegend == null) {
      showLegend = DEFAULT_SHOW_LEGEND;
      UIState.INSTANCE.putShowLegendAll(component.name(), showLegend);
    }
    view.getRealTimeLegendPanel().setSelected(showLegend);

    Boolean showConfig = UIState.INSTANCE.getShowConfigAll(component.name());
    if (showConfig == null) {
      showConfig = DEFAULT_SHOW_CONFIG;
      UIState.INSTANCE.putShowConfigAll(component.name(), showConfig);
    }
    view.getConfigShowHidePanel().setSelected(showConfig);
  }

  private void setupListeners() {
    view.getRealTimeRangePanel().setRunAction((action, range) -> handleRealTimeRange(range));
    view.getRealTimeLegendPanel().setStateChangeConsumer(s -> handleLegend(s == ChartLegendState.SHOW));
    view.getConfigShowHidePanel().setStateChangeConsumer(s -> handleConfig(s == ChartConfigState.SHOW));
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapse);
  }

  private void handleRealTimeRange(RangeRealTime range) {
    log.info("PreviewConfig – real-time range changed to {}", range);
    UIState.INSTANCE.putRealTimeRangeAll(component.name(), range);
    sendMessage(Action.REALTIME_RANGE_CHANGE, "range", range);
  }

  private void handleLegend(boolean show) {
    log.info("PreviewConfig – legend visibility {}", show);
    UIState.INSTANCE.putShowLegendAll(component.name(), show);
    sendMessage(Action.CHART_LEGEND_STATE_ALL, "chartLegendState",
                show ? ChartLegendState.SHOW : ChartLegendState.HIDE);
  }

  private void handleConfig(boolean show) {
    log.info("PreviewConfig – config visibility {}", show);
    UIState.INSTANCE.putShowConfigAll(component.name(), show);
    sendMessage(Action.SHOW_HIDE_CONFIG_ALL, "configState",
                show ? ChartConfigState.SHOW : ChartConfigState.HIDE);
  }

  private void handleCollapse(ChartCardState state) {
    log.info("PreviewConfig – dashboard collapse {}", state);
    UIState.INSTANCE.putChartCardStateAll(component.name(), state);
    sendMessage(Action.EXPAND_COLLAPSE_ALL, "cardState", state);
  }

  private void sendMessage(Action action, String key, Object value) {
    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(action)
                           .parameter(key, value)
                           .build());
  }
}
