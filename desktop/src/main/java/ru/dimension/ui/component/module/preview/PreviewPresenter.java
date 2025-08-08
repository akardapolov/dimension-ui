package ru.dimension.ui.component.module.preview;

import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.PreviewChartModule;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;

@Log4j2
public class PreviewPresenter {
  private final PreviewModel model;
  private final PreviewView view;

  private final MessageBroker.Component component = Component.PREVIEW;
  private RangeRealTime range = RangeRealTime.TEN_MIN;
  private boolean showLegend = true;
  private boolean showConfig = true;
  private boolean isExpand = true;

  public PreviewPresenter(PreviewModel model,
                          PreviewView view) {
    this.model = model;
    this.view = view;

    initialize();
    setupListeners();
  }

  private void initialize() {
    QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(model.getKey().getQueryId());
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());

    view.updateColumnTables(tableInfo);

    RangeRealTime range = UIState.INSTANCE.getRealTimeRangeAll(component.name());
    this.range = range == null ? this.range : range;
    view.getRealTimeRangePanel().setSelectedRange(this.range);

    Boolean showLegend = UIState.INSTANCE.getShowLegendAll(component.name());
    this.showLegend = showLegend != null ? showLegend : true;
    view.getRealTimeLegendPanel().setSelected(this.showLegend);

    Boolean showConfig = UIState.INSTANCE.getShowConfigAll(component.name());
    this.showConfig = showConfig != null ? showConfig : true;
    view.getConfigShowHidePanel().setSelected(this.showConfig);

    updateRealTimeChart();

    view.setCheckboxChangeListener(this::handleCheckboxChange);
  }

  private void setupListeners() {
    view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);
    view.getRealTimeLegendPanel().setStateChangeConsumer(showLegend ->
                                                             handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));
    view.getConfigShowHidePanel().setStateChangeConsumer(showConfig ->
                                                             handleConfigChartChange(ChartConfigState.SHOW.equals(showConfig)));
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleCheckboxChange(String columnName, boolean selected) {
    if (selected) {
      addChartForColumn(columnName);
    } else {
      removeChartForColumn(columnName);
    }
  }

  private void addChartForColumn(String columnName) {
    if (model.getChartModuleByColumnName(columnName).isPresent()) {
      return;
    }

    TableInfo tableInfo = model.getTableInfo();
    if (tableInfo == null) return;

    tableInfo.getCProfiles().stream()
        .filter(cProfile -> cProfile.getColName().equals(columnName))
        .findFirst()
        .ifPresent(cProfile -> {
          view.setColumnSelected(columnName, true); // Immediate UI feedback
          addChartModule(new Metric(tableInfo, cProfile));
        });
  }

  private void removeChartForColumn(String columnName) {
    model.getChartModuleByColumnName(columnName).ifPresent(module -> {
      if (module.getValue() != null) {
        view.removeChartCard(module.getValue());
        model.removeChartModule(module.getKey());
      }
    });
  }

  public void handleRealTimeRangeChange(String action,
                                        RangeRealTime range) {
    log.info("Preview range changed to: {}", range);
    this.range = range;
    UIState.INSTANCE.putRealTimeRangeAll(component.name(), range);
    model.getChartModules().values().forEach(module -> module.handleRealTimeRange(range));
  }

  public void handleLegendChange(Boolean showLegend) {
    log.info("Preview legend visibility changed to: {}", showLegend);
    this.showLegend = showLegend;
    UIState.INSTANCE.putShowLegendAll(component.name(), showLegend);

    ChartLegendState state = showLegend ? ChartLegendState.SHOW : ChartLegendState.HIDE;
    model.getChartModules().values().forEach(module -> module.handleLegendChange(state));
  }

  public void handleConfigChartChange(Boolean showConfig) {
    log.info("Preview config chart visibility changed to: {}", showConfig);
    this.showConfig = showConfig;
    UIState.INSTANCE.putShowConfigAll(component.name(), showConfig);

    ChartConfigState state = showConfig ? ChartConfigState.SHOW : ChartConfigState.HIDE;
    model.getChartModules().values().forEach(module -> module.handleChartConfigState(state));
  }

  private void handleCollapseCardChange(ChartCardState state) {
    log.info("Set card state in " + component.name() + " to: {}", state);
    this.isExpand = ChartCardState.EXPAND_ALL.equals(state);
    model.getChartModules().values().forEach(module -> module.setCollapsed(isExpand));
  }

  private void updateRealTimeChart() {
    view.clearAllCharts();
    model.clearChartModules();

    TableInfo tableInfo = model.getTableInfo();
    if (tableInfo == null) {
      return;
    }

    tableInfo.getCProfiles().stream()
        .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
        .forEach(cProfile -> {
          Metric metric = new Metric(tableInfo, cProfile);
          addChartModule(metric);
        });
  }

  private void addChartModule(Metric metric) {
    ProfileTaskQueryKey key = model.getKey();
    CProfile cProfile = metric.getYAxis();
    ChartKey chartKey = new ChartKey(key, cProfile);

    QueryInfo queryInfo = model.getQueryInfo();
    ChartInfo chartInfo = model.getChartInfo();
    TableInfo tableInfo = model.getTableInfo();
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();

    PreviewChartModule taskPane = new PreviewChartModule(Component.PREVIEW, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    taskPane.setTitle(cProfile.getColName());

    view.setColumnSelected(cProfile.getColName(), false);

    view.addChartCard(taskPane, (module, error) -> {
      if (error != null) {
        log.error("Failed to load preview chart", error);
        return;
      }
      model.addChartModule(cProfile, module);
      module.handleRealTimeRangeUI(range);
      module.handleLegendChange(showLegend ? ChartLegendState.SHOW : ChartLegendState.HIDE);
      module.handleChartConfigState(showConfig ? ChartConfigState.SHOW : ChartConfigState.HIDE);
      module.setCollapsed(!isExpand);
    });
  }
}