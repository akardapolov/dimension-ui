package ru.dimension.ui.component.module.preview.charts;

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
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.ChartModule;
import ru.dimension.ui.component.module.PreviewChartModule;
import ru.dimension.ui.component.module.preview.chart.ChartDetailDialog;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PreviewChartsPresenter implements MessageAction, CollectStartStopListener {

  private final MessageBroker.Component component;

  private final PreviewChartsModel model;
  private final PreviewChartsView view;
  private final MessageBroker broker = MessageBroker.getInstance();

  public PreviewChartsPresenter(MessageBroker.Component component,
                                PreviewChartsModel model,
                                PreviewChartsView view) {
    this.component = component;
    this.model = model;
    this.view = view;
  }

  @Override
  public void receive(Message message) {
    log.info("Message received >>> " + message.destination() + " with action >>> " + message.action());

    switch (message.action()) {
      case REALTIME_RANGE_CHANGE -> handleRealTimeRangeChange(message);
      case ADD_CHART -> handleAddChart(message);
      case REMOVE_CHART -> handleRemoveChart(message);
      case CHART_LEGEND_STATE_ALL -> chartLegendStateAll(message);
      case SHOW_HIDE_CONFIG_ALL -> showHideConfigState(message);
      case EXPAND_COLLAPSE_ALL -> expandCollapseAll(message);
      case SHOW_CHART_FULL -> handleShowChartFull(message);
    }
  }

  private void handleShowChartFull(Message message) {
    ChartKey chartKey = message.parameters().get("chartKey");
    ProfileTaskQueryKey key = message.parameters().get("key");
    Metric metric = message.parameters().get("metric");
    QueryInfo queryInfo = message.parameters().get("queryInfo");
    ChartInfo chartInfo = message.parameters().get("chartInfo");
    TableInfo tableInfo = message.parameters().get("tableInfo");

    ChartModule chartModule = new ChartModule(
        component, chartKey, key, metric, queryInfo, chartInfo, tableInfo,
        model.getSqlQueryState(), model.getDStore()
    );

    String keyValue = KeyHelper.getKey(model.getProfileManager(), key, chartKey.getCProfile());
    chartModule.setTitle(keyValue);

    chartModule.initializeUI().run();

    ChartDetailDialog dialog = new ChartDetailDialog(chartModule);
    model.setChartDetailDialog(dialog);

    Destination destinationRealtime = getDestination(Panel.REALTIME, chartKey);
    Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
    broker.addReceiver(destinationRealtime, model.getChartDetailDialog().getChartModule().getPresenter());
    broker.addReceiver(destinationHistory, model.getChartDetailDialog().getChartModule().getPresenter());

    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosed(java.awt.event.WindowEvent e) {

        Destination destinationRealtime = getDestination(Panel.REALTIME, chartKey);
        Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
        broker.deleteReceiver(destinationRealtime, model.getChartDetailDialog().getChartModule().getPresenter());
        broker.deleteReceiver(destinationHistory, model.getChartDetailDialog().getChartModule().getPresenter());

        model.setChartDetailDialog(null);
      }
    });

    dialog.setVisible(true);
  }

  private void showHideConfigState(Message message) {
    ChartConfigState chartConfigState = message.parameters().get("configState");
    model.getChartPanes().forEach((key, chartMap) -> chartMap.values()
        .forEach(chartModule -> chartModule.handleChartConfigState(chartConfigState)));
  }

  private void handleRealTimeRangeChange(Message message) {
    RangeRealTime realTimeRange = message.parameters().get("range");
    model.getChartPanes().forEach((key, chartMap) -> chartMap.values()
        .forEach(chartModule -> chartModule.handleRealTimeRange(realTimeRange)));
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
    if (model == null || model.getChartPanes() == null) {
      return;
    }

    ChartCardState cardState = message.parameters().get("cardState");

    model.getChartPanes().forEach((key, val) -> {
      val.values().forEach(chartModule -> chartModule.setCollapsed(ChartCardState.EXPAND_ALL.equals(cardState)));
    });
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

    PreviewChartModule taskPane = new PreviewChartModule(component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);

    String keyValue = KeyHelper.getKey(model.getProfileManager(), key, cProfile);
    taskPane.setTitle(keyValue);

    log.info("Add task pane: " + keyValue);

    view.addChartCard(taskPane, (module, error) -> {
      if (error != null) {
        DialogHelper.showErrorDialog("Failed to load chart: " + error.getMessage(), "Error", error);
        return;
      }
      model.getChartPanes().computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(cProfile, taskPane);

      taskPane.revalidate();
      taskPane.repaint();

      RangeRealTime rangeRealTimeGlobal = UIState.INSTANCE.getRealTimeRangeAll(component.name());
      RangeRealTime rangeRealTime = UIState.INSTANCE.getRealTimeRange(chartKey);
      taskPane.handleRealTimeRangeUI(rangeRealTime == null ? rangeRealTimeGlobal : rangeRealTime);

      ChartCardState chartCardState = UIState.INSTANCE.getChartCardStateAll(component.name());
      taskPane.setCollapsed(ChartCardState.EXPAND_ALL.equals(chartCardState));
    });
  }

  private void handleRemoveChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    Metric metric = message.parameters().get("metric");
    CProfile cProfile = metric.getYAxis();

    PreviewChartModule taskPane = model.getChartPanes().get(key).get(cProfile);

    try {
      logChartAction(message.action(), cProfile);
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

      ChartDetailDialog dialog = model.getChartDetailDialog();
      if (dialog != null && dialog.isVisible()) {
        ChartModule dialogChartModule = dialog.getChartModule();
        if (dialogChartModule.getModel().getKey().equals(profileTaskQueryKey) &&
            dialogChartModule.isReadyRealTimeUpdate()) {
          try {
            dialogChartModule.loadData();
          } catch (Exception e) {
            log.error("Error loading data in dialog", e);
          }
        }
      }
  }

  private void logChartAction(Action action,
                              CProfile cProfile) {
    log.info("Message action: " + action + " for " + cProfile);
  }
}
