package ru.dimension.ui.component.module.adhoc.charts;

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
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.model.AdHocChartKey;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.state.AdHocStateManager;

@Log4j2
public class AdHocChartsPresenter implements MessageAction {

  private final AdHocChartsModel model;
  private final AdHocChartsView view;

  private final MessageBroker broker = MessageBroker.getInstance();
  private final AdHocStateManager adHocStateManager = AdHocStateManager.getInstance();

  public AdHocChartsPresenter(AdHocChartsModel model,
                              AdHocChartsView view) {
    this.model = model;
    this.view = view;

    view.setTabChangeListener(this::handleTabChange);
  }

  private void handleTabChange(String globalKey) {
    if (globalKey != null && !globalKey.isEmpty()) {
      broker.sendMessage(Message.builder()
                             .destination(Destination.withDefault(Component.ADHOC, Module.CONFIG))
                             .action(Action.HISTORY_CUSTOM_UI_RANGE_CHANGE)
                             .parameter("globalKey", globalKey)
                             .build());
    }
  }

  @Override
  public void receive(Message message) {
    String globalKey = message.parameters().get("globalKey");
    model.setGlobalKey(globalKey);

    switch (message.action()) {
      case HISTORY_RANGE_CHANGE -> handleHistoryRangeChange(message);
      case ADD_CHART -> handleAddChart(message);
      case REMOVE_CHART -> handleRemoveChart(message);
      case SHOW_HIDE_DETAIL_ALL -> handleDetailVisibilityChange(message);
      case CHART_LEGEND_STATE_ALL -> chartLegendStateAll(message);
      case EXPAND_COLLAPSE_ALL -> expandCollapseAll(message);
    }
  }

  private void handleHistoryRangeChange(Message message) {
    RangeHistory historyRange = message.parameters().get("range");
    ChartRange chartRange = message.parameters().get("chartRange");

    model.getChartPanes().forEach((key, chartMap) -> {
      String chartGlobalKey = key.getConnectionId() + "_" + key.getTableName();
      if (chartGlobalKey.equals(model.getGlobalKey())) {
        chartMap.values().forEach(chartModule -> {
          if (RangeHistory.CUSTOM.equals(historyRange)) {
            chartModule.updateHistoryCustomRange(chartRange);
          } else {
            chartModule.updateHistoryRange(historyRange);
          }
        });
      }
    });
  }

  private void handleDetailVisibilityChange(Message message) {
    DetailState detailState = message.parameters().get("detailState");
    if (model == null || model.getChartPanes() == null) return;

    model.getChartPanes().forEach((key, value) -> {
      String chartGlobalKey = key.getConnectionId() + "_" + key.getTableName();
      if (chartGlobalKey.equals(model.getGlobalKey())) {
        value.values().forEach(chartModule -> chartModule.setDetailState(detailState));
      }
    });
  }

  private void chartLegendStateAll(Message message) {
    if (model == null || model.getChartPanes() == null) return;

    ChartLegendState chartLegendState = message.parameters().get("chartLegendState");
    model.getChartPanes().forEach((key, value) -> {
      String chartGlobalKey = key.getConnectionId() + "_" + key.getTableName();
      if (chartGlobalKey.equals(model.getGlobalKey())) {
        value.values().forEach(chartModule -> chartModule.handleLegendChange(chartLegendState));
      }
    });
  }

  private void expandCollapseAll(Message message) {
    if (model == null || model.getChartPanes() == null) return;

    ChartCardState cardState = message.parameters().get("cardState");
    model.getChartPanes().forEach((key, value) -> {
      String chartGlobalKey = key.getConnectionId() + "_" + key.getTableName();
      if (chartGlobalKey.equals(model.getGlobalKey())) {
        value.values().forEach(chartModule ->
                                   chartModule.setCollapsed(ChartCardState.EXPAND_ALL.equals(cardState))
        );
      }
    });
  }

  private void handleAddChart(Message message) {
    logChartAction(message);

    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
    String activeTab = message.parameters().get("activeTab");
    String tableName = message.parameters().get("tableName");
    CProfile cProfile = message.parameters().get("cProfile");
    TableInfo tableInfo = message.parameters().get("tableInfo");
    QueryInfo queryInfo = message.parameters().get("queryInfo");
    ChartInfo chartInfo = message.parameters().get("chartInfo");

    Metric metric = new Metric(tableInfo, cProfile);

    AdHocKey adHocKey = new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());

    DStore dStore = model.getAdHocDatabaseManager().getDataBase(connectionInfo);

    AdHocChartModule taskPane =
        new AdHocChartModule(adHocKey, metric, queryInfo, chartInfo, tableInfo, dStore);

    String keyValue = getKey(cProfile);

    taskPane.setTitle(keyValue);

    String adHocTabKey = getAdHocTabKey(connectionInfo, activeTab, tableName);
    String globalKey = KeyHelper.getGlobalKey(connectionInfo.getId(), tableName);

    view.addChartCard(
        adHocTabKey,
        globalKey,
        taskPane,
        (module, error) -> {
          if (error != null) {
            DialogHelper.showErrorDialog("Failed to load chart: " + error.getMessage(), "Error", error);
            return;
          }
          model.getChartPanes()
              .computeIfAbsent(adHocKey, k -> new ConcurrentHashMap<>())
              .put(cProfile, taskPane);

          Destination destination = getDestination(adHocKey);
          broker.addReceiver(destination, taskPane.getPresenter());

          taskPane.revalidate();
          taskPane.repaint();

          ChartCardState chartCardState = adHocStateManager.getChartCardStateAll(globalKey);
          taskPane.setCollapsed(ChartCardState.EXPAND_ALL.equals(chartCardState));
        }
    );
  }

  private void handleRemoveChart(Message message) {
    logChartAction(message);

    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
    String activeTab = message.parameters().get("activeTab");
    String tableName = message.parameters().get("tableName");
    CProfile cProfile = message.parameters().get("cProfile");

    AdHocKey adHocKey = new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());
    String adHocTabKey = getAdHocTabKey(connectionInfo, activeTab, tableName);

    AdHocChartModule taskPane = model.getChartPanes().get(adHocKey).get(cProfile);

    try {
      Destination destination = getDestination(adHocKey);
      broker.deleteReceiver(destination, taskPane.getPresenter());
    } finally {
      log.info("Remove task pane: " + taskPane.getTitle());
      view.removeChartCard(adHocTabKey, taskPane);
      model.getChartPanes().get(adHocKey).remove(cProfile);
    }
  }

  public String getKey(CProfile cProfile) {
    String columnName = cProfile.getColName();

    String keyValue = String.format("Column: %s", columnName);

    return keyValue.length() > 300 ? keyValue.substring(0, 300) + " ... " : keyValue;
  }

  public String getAdHocTabKey(ConnectionInfo connectionInfo, String activeTab, String tableName) {
    String connName = connectionInfo.getName();
    return String.format("<html><b>Connection:</b> %s<br><b>%s:</b> %s</html>",
                         connName, activeTab, tableName);
  }

  private static Destination getDestination(AdHocKey adHocKey) {
    return Destination.builder()
        .component(Component.ADHOC)
        .module(Module.CHART)
        .panel(Panel.HISTORY)
        .block(Block.CHART)
        .chartKey(new AdHocChartKey(adHocKey))
        .build();
  }

  private void logChartAction(Message message) {
    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
    String activeTab = message.parameters().get("activeTab");
    String tableName = message.parameters().get("tableName");
    CProfile cProfile = message.parameters().get("cProfile");
    ChartInfo chartInfo = message.parameters().get("chartInfo");

    log.info("Message action: {}\nfor\n{}\n{}\n{}\n{}\n{}",
             message.action(),
             connectionInfo,
             activeTab,
             tableName,
             cProfile,
             chartInfo);
  }
}