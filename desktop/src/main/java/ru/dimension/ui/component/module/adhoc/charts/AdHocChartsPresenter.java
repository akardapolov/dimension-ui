package ru.dimension.ui.component.module.adhoc.charts;

import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.model.DetailState;

@Log4j2
public class AdHocChartsPresenter implements MessageAction {

  private final AdHocChartsModel model;
  private final AdHocChartsView view;

  public AdHocChartsPresenter(AdHocChartsModel model,
                              AdHocChartsView view) {
    this.model = model;
    this.view = view;
  }

  @Override
  public void receive(Message message) {
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

    if (RangeHistory.CUSTOM.equals(historyRange)) {
      model.getChartPanes().forEach((key, chartMap) ->
                                        chartMap.values().forEach(chartModule ->
                                                                      chartModule.updateHistoryCustomRange(chartRange)
                                        )
      );
    } else {
      model.getChartPanes().forEach((key, chartMap) ->
                                        chartMap.values().forEach(chartModule ->
                                                                      chartModule.updateHistoryRange(historyRange)
                                        ));
    }
  }

  private void handleDetailVisibilityChange(Message message) {
    DetailState detailState = message.parameters().get("detailState");
    if (model == null || model.getChartPanes() == null) return;

    model.getChartPanes().forEach((key, value) -> {
      value.values().forEach(chartModule -> chartModule.setDetailState(detailState)
      );
    });
  }

  private void chartLegendStateAll(Message message) {
    if (model == null || model.getChartPanes() == null) {
      return;
    }

    ChartLegendState chartLegendState = message.parameters().get("chartLegendState");

    model.getChartPanes().forEach((key, value) -> {
      value.values().forEach(chartModule -> chartModule.handleLegendChange(chartLegendState));
    });
  }

  private void expandCollapseAll(Message message) {
    if (model == null || model.getChartPanes() == null) {
      return;
    }

    ChartCardState cardState = message.parameters().get("cardState");

    model.getChartPanes().forEach((key, value) -> {
      value.values().forEach(chartModule -> chartModule.setCollapsed(ChartCardState.EXPAND_ALL.equals(cardState)));
    });
  }

  private void handleAddChart(Message message) {
    logChartAction(message);

    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
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

    String keyValue = getKey(connectionInfo, tableName, cProfile);

    taskPane.setTitle(keyValue);

    view.addChartCard(
        taskPane,
        (module, error) -> {
          if (error != null) {
            DialogHelper.showErrorDialog("Failed to load chart: " + error.getMessage(), "Error", error);
            return;
          }
          model.getChartPanes()
              .computeIfAbsent(adHocKey, k -> new ConcurrentHashMap<>())
              .put(cProfile, taskPane);

          taskPane.revalidate();
          taskPane.repaint();
        }
    );
  }

  private void handleRemoveChart(Message message) {
    logChartAction(message);

    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
    String tableName = message.parameters().get("tableName");
    CProfile cProfile = message.parameters().get("cProfile");

    AdHocKey adHocKey = new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());

    AdHocChartModule taskPane = model.getChartPanes().get(adHocKey).get(cProfile);
    model.getChartPanes().get(adHocKey).remove(cProfile);

    view.removeChartCard(taskPane);
  }

  public String getKey(ConnectionInfo connectionInfo, String tableName, CProfile cProfile) {
    String connName = connectionInfo.getName();
    String columnName = cProfile.getColName();

    String keyValue = String.format("Connection: %s >>> Table: %s >>> Column: %s", connName, tableName, columnName);

    return keyValue.length() > 300 ? keyValue.substring(0, 300) + " ... " : keyValue;
  }

  private void logChartAction(Message message) {
    ConnectionInfo connectionInfo = message.parameters().get("connectionInfo");
    String tableName = message.parameters().get("tableName");
    CProfile cProfile = message.parameters().get("cProfile");

    log.info("Message action: {}\nfor\n{}\n{}\n{}",
             message.action(),
             connectionInfo,
             tableName,
             cProfile);
  }
}