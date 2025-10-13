package ru.dimension.ui.component.module.charts;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.module.ChartModule;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;

@Log4j2
public class ChartsPresenter implements MessageAction, CollectStartStopListener {
  private final MessageBroker.Component component;
  private final ChartsModel model;
  private final ChartsView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ChartsPresenter(MessageBroker.Component component,
                         ChartsModel model,
                         ChartsView view) {
    this.component = component;
    this.model = model;
    this.view = view;
  }

  @Override
  public void receive(Message message) {
    switch (message.action()) {
      case CHANGE_TAB -> handleTabChange(message);
      case REALTIME_RANGE_CHANGE -> handleRealTimeRangeChange(message);
      case HISTORY_RANGE_CHANGE -> handleHistoryRangeChange(message);
      case ADD_CHART -> handleAddChart(message);
      case REMOVE_CHART -> handleRemoveChart(message);
      case SHOW_HIDE_DETAIL_ALL -> handleDetailVisibilityChange(message);
      case CHART_LEGEND_STATE_ALL -> chartLegendStateAll(message);
      case EXPAND_COLLAPSE_ALL -> expandCollapseAll(message);
    }
  }

  private void handleTabChange(Message message) {
    PanelTabType panelTabType = message.parameters().get("panelTabType");
    model.getChartPanes()
        .forEach((key, chartMap) -> chartMap.values().forEach(chartModule -> chartModule.setActiveTab(panelTabType)));
  }

  private void handleRealTimeRangeChange(Message message) {
    RangeRealTime realTimeRange = message.parameters().get("range");
    model.getChartPanes().forEach((key, chartMap) -> chartMap.values()
        .forEach(chartModule -> chartModule.updateRealTimeRange(realTimeRange)));
  }

  private void handleHistoryRangeChange(Message message) {
    RangeHistory historyRange = message.parameters().get("range");
    ChartRange chartRange = message.parameters().get("chartRange");

    if (RangeHistory.CUSTOM.equals(historyRange)) {
      model.getChartPanes().forEach((key, chartMap) -> chartMap.values()
          .forEach(chartModule -> chartModule.updateHistoryCustomRange(chartRange)));
    } else {
      model.getChartPanes().forEach((key, chartMap) -> chartMap.values()
          .forEach(chartModule -> chartModule.updateHistoryRange(historyRange)));
    }
  }

  private void handleDetailVisibilityChange(Message message) {
    DetailState detailState = message.parameters().get("detailState");
    if (model == null || model.getChartPanes() == null) {
      return;
    }

    model.getChartPanes().forEach((key, val) -> {
      val.values().forEach(chartModule -> chartModule.setDetailState(detailState));
    });
  }

  private void chartLegendStateAll(Message message) {
    if (model == null || model.getChartPanes() == null) {
      return;
    }

    ChartLegendState chartLegendState = message.parameters().get("chartLegendState");

    model.getChartPanes().forEach((key, val) -> {
      val.values().forEach(chartModule -> chartModule.handleLegendChange(chartLegendState));
    });
  }

  private void expandCollapseAll(Message message) {
    if (model == null || model.getChartPanes() == null) return;

    ChartCardState cardState = message.parameters().get("cardState");
    boolean shouldCollapse = !ChartCardState.EXPAND_ALL.equals(cardState);

    model.setProgrammaticChange(true);
    try {
      model.getChartPanes().values().stream()
          .flatMap(map -> map.values().stream())
          .forEach(chartModule -> chartModule.setCollapsed(shouldCollapse));
    } finally {
      model.setProgrammaticChange(false);
    }
  }

  private void handleAddChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    Metric metric = message.parameters().get("metric");
    CProfile cProfile = metric.getYAxis();
    ChartKey chartKey = new ChartKey(key, cProfile);

    QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(key.getQueryId());
    ChartInfo chartInfo = model.getProfileManager().getChartInfoById(key.getQueryId());
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();

    ChartModule taskPane = new ChartModule(component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);

    String keyValue = KeyHelper.getKey(model.getProfileManager(), key, cProfile);
    taskPane.setTitle(keyValue);

    log.info("Add task pane: " + keyValue);

    PropertyChangeListener collapseListener = evt -> {
      if (!model.isProgrammaticChange() &&
          "collapsed".equals(evt.getPropertyName()) &&
          Boolean.FALSE.equals(evt.getNewValue())) {
        view.centerComponentInScrollPane(taskPane);
      }
    };
    taskPane.addPropertyChangeListener("collapsed", collapseListener);
    model.getCollapseListeners().put(taskPane, collapseListener);

    view.addChartCard(taskPane, (module, error) -> {
      if (error != null) {
        PropertyChangeListener listener = model.getCollapseListeners().remove(taskPane);
        if (listener != null) {
          taskPane.removePropertyChangeListener("collapsed", listener);
        }
        DialogHelper.showErrorDialog("Failed to load chart: " + error.getMessage(), "Error", error);
        return;
      }

      model.getChartPanes().computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(cProfile, taskPane);

      Destination destinationRealtime = getDestination(Panel.REALTIME, chartKey);
      Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);

      broker.addReceiver(destinationRealtime, taskPane.getPresenter());
      broker.addReceiver(destinationHistory, taskPane.getPresenter());

      taskPane.revalidate();
      taskPane.repaint();

      ChartCardState chartCardState = UIState.INSTANCE.getChartCardStateAll(component.name());
      taskPane.setCollapsed(ChartCardState.EXPAND_ALL.equals(chartCardState));
    });
  }

  private void handleRemoveChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    Metric metric = message.parameters().get("metric");
    CProfile cProfile = metric.getYAxis();

    ChartModule taskPane = model.getChartPanes().get(key).get(cProfile);

    PropertyChangeListener listener = model.getCollapseListeners().remove(taskPane);
    if (listener != null) {
      taskPane.removePropertyChangeListener("collapsed", listener);
    }

    try {
      logChartAction(message.action(), cProfile);

      ChartKey chartKey = new ChartKey(key, cProfile);

      Destination destinationRealtime = getDestination(Panel.REALTIME, chartKey);
      Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);

      broker.deleteReceiver(destinationRealtime, taskPane.getPresenter());
      broker.deleteReceiver(destinationHistory, taskPane.getPresenter());
    } finally {
      log.info("Remove task pane: " + taskPane.getTitle());
      view.removeChartCard(taskPane);
      model.getChartPanes().get(key).remove(cProfile);
    }
  }

  private Destination getDestination(Panel realtime,
                                     ChartKey chartKey) {
    return Destination.builder().component(component)
        .module(Module.CHART)
        .panel(realtime)
        .block(Block.CHART)
        .chartKey(chartKey).build();
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Start collect for {}", profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Stop collect for {}", profileTaskQueryKey);

    if (model == null || model.getChartPanes() == null) {
      return;
    }

    if (model.getChartPanes().get(profileTaskQueryKey) == null || model.getChartPanes().get(profileTaskQueryKey)
        .isEmpty()) {
      return;
    }

    model.getChartPanes().get(profileTaskQueryKey).forEach((key, val) -> {
      if (val.isReadyRealTimeUpdate()) {
        try {
          val.loadData();
        } catch (Exception e) {
          log.error("Error loading data", e);
        }
      }
    });
  }

  public boolean isRunning(int profileId) {
    return model.getChartPanes().entrySet().stream().anyMatch(f -> f.getKey().getProfileId() == profileId);
  }

  private void logChartAction(Action action,
                              CProfile cProfile) {
    log.info("Message action: " + action + " for " + cProfile);
  }
}
