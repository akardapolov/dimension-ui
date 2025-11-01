package ru.dimension.ui.component.module.preview;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.preview.spi.IHistoryPreviewChart;
import ru.dimension.ui.component.module.preview.spi.IPreviewChart;
import ru.dimension.ui.component.module.preview.spi.IRealTimePreviewChart;
import ru.dimension.ui.component.module.preview.spi.PreviewChartFactory;
import ru.dimension.ui.component.module.preview.spi.PreviewMode;
import ru.dimension.ui.component.module.preview.spi.RunMode;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.AdHocStateManager;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
public class PreviewPresenter {
  private final PreviewModel model;
  private final PreviewView view;
  private final MessageBroker.Component component = Component.PREVIEW;

  private final PreviewMode mode;
  private final PreviewChartFactory chartFactory;

  private RangeRealTime rangeRealTime = RangeRealTime.TEN_MIN;
  private RangeHistory rangeHistory = RangeHistory.DAY;

  private boolean showLegend = true;
  private boolean showConfig = true;
  private boolean isExpand = true;

  public PreviewPresenter(PreviewModel model,
                          PreviewView view,
                          PreviewMode mode,
                          PreviewChartFactory chartFactory) {
    this.model = model;
    this.view = view;
    this.mode = mode;
    this.chartFactory = chartFactory;

    initialize();
    setupListeners();
  }

  private void initialize() {
    TableInfo tableInfo = model.getTableInfo();

    view.updateColumnTables(tableInfo);

    if (mode == PreviewMode.PREVIEW) {
      RangeRealTime range = UIState.INSTANCE.getRealTimeRangeAll(component.name());
      this.rangeRealTime = range == null ? this.rangeRealTime : range;
      view.getRealTimeRangePanel().setSelectedRange(this.rangeRealTime);
      view.getRealTimeRangePanel().setVisible(true);
      view.getHistoryRangePanel().setVisible(false);
    } else {
      RangeHistory range = this.model.getChartInfo().getRangeHistory();
      if (RangeHistory.CUSTOM.equals(range)) {
        ChartRange customRange = new ChartRange(model.getChartInfo().getCustomBegin(),
                                                model.getChartInfo().getCustomEnd());
        view.getHistoryRangePanel().getDateTimePickerFrom().setDate(new Date(customRange.getBegin()));
        view.getHistoryRangePanel().getDateTimePickerTo().setDate(new Date(customRange.getEnd()));
      }

      view.getHistoryRangePanel().setSelectedRange(Objects.isNull(range) ? rangeHistory : range);
      view.getRealTimeRangePanel().setVisible(false);
      view.getHistoryRangePanel().setVisible(true);
    }

    Boolean showLegend = UIState.INSTANCE.getShowLegendAll(component.name());
    this.showLegend = showLegend != null ? showLegend : true;
    view.getRealTimeLegendPanel().setSelected(this.showLegend);

    Boolean showConfig = UIState.INSTANCE.getShowConfigAll(component.name());
    this.showConfig = showConfig != null ? showConfig : true;
    view.getConfigShowHidePanel().setSelected(this.showConfig);

    updateCharts();
    view.setCheckboxChangeListener(this::handleCheckboxChange);
  }

  private void setupListeners() {
    if (mode == PreviewMode.PREVIEW) {
      view.getRealTimeRangePanel().setRunAction(this::handleRealTimeRangeChange);
    } else {
      view.getHistoryRangePanel().setRunAction(this::handleHistoryRangeChange);
      view.getHistoryRangePanel().getButtonApplyRange().addActionListener(e -> handleCustomHistoryRangeChange());
    }
    view.getRealTimeLegendPanel().setStateChangeConsumer(
        showLegend -> handleLegendChange(ChartLegendState.SHOW.equals(showLegend)));
    view.getConfigShowHidePanel().setStateChangeConsumer(
        showConfig -> handleConfigChartChange(ChartConfigState.SHOW.equals(showConfig)));
    view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleCustomHistoryRangeChange() {
    ChartRange chartRange = getChartRangeFromPickers();
    log.info("Custom history chart range changed to: {}", chartRange);

    model.getChartInfo().setCustomBegin(chartRange.getBegin());
    model.getChartInfo().setCustomEnd(chartRange.getEnd());

    handleHistoryRangeChange("", RangeHistory.CUSTOM);
  }

  public void handleHistoryRangeChange(String action, RangeHistory range) {
    if (mode == PreviewMode.PREVIEW) return;
    log.info("Preview history range changed to: {}", range);

    UIState.INSTANCE.putHistoryRangeAll(component.name(), range);

    model.getChartModules().values().forEach(ch -> {
      if (ch instanceof IHistoryPreviewChart rt) {
        if (RangeHistory.CUSTOM.equals(range)) {
          ChartRange customRange = new ChartRange(model.getChartInfo().getCustomBegin(),
                                                  model.getChartInfo().getCustomEnd());

          rt.handleHistoryCustomRange(customRange);
          rt.handleHistoryRangeUI(RangeHistory.CUSTOM);
        }
        rt.handleHistoryRange(range);
      }
    });

    view.getHistoryRangePanel().setSelectedRange(range);
  }

  private ChartRange getChartRangeFromPickers() {
    DateTimePicker fromPicker = view.getHistoryRangePanel().getDateTimePickerFrom();
    DateTimePicker toPicker = view.getHistoryRangePanel().getDateTimePickerTo();

    Date fromDate = fromPicker.getDate();
    Date toDate = toPicker.getDate();

    return new ChartRange(fromDate.getTime(), toDate.getTime());
  }

  private void handleCheckboxChange(String columnName, boolean selected) {
    if (selected) {
      addChartForColumn(columnName);
    } else {
      removeChartForColumn(columnName);
    }
  }

  private void addChartForColumn(String columnName) {
    if (model.getChartModuleByColumnName(columnName).isPresent()) return;

    TableInfo tableInfo = model.getTableInfo();
    if (tableInfo == null) return;

    tableInfo.getCProfiles().stream()
        .filter(cProfile -> cProfile.getColName().equals(columnName))
        .findFirst()
        .ifPresent(cProfile -> {
          view.setColumnSelected(columnName, true);
          addChartModule(new Metric(tableInfo, cProfile));
        });
  }

  private void removeChartForColumn(String columnName) {
    model.getChartModuleByColumnName(columnName).ifPresent(entry -> {
      IPreviewChart module = entry.getValue();
      view.removeChartCard(module);
      model.removeChartModule(entry.getKey());
    });
  }

  public void handleRealTimeRangeChange(String action, RangeRealTime range) {
    if (mode != PreviewMode.PREVIEW) return;
    log.info("Preview range changed to: {}", range);
    this.rangeRealTime = range;
    UIState.INSTANCE.putRealTimeRangeAll(component.name(), range);

    model.getChartModules().values().forEach(ch -> {
      if (ch instanceof IRealTimePreviewChart rt) {
        rt.handleRealTimeRange(range);
      }
    });
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
    log.info("Set card state in {} to: {}", component.name(), state);
    this.isExpand = ChartCardState.EXPAND_ALL.equals(state);
    model.getChartModules().values().forEach(module -> module.setCollapsed(isExpand));
  }

  private void updateCharts() {
    view.clearAllCharts();
    model.clearChartModules();

    TableInfo tableInfo = model.getTableInfo();
    if (tableInfo == null) {
      return;
    }

    Metric metric = model.getMetric();
    String metricColName = null;
    if (metric != null) {
      addChartModule(metric);
      metricColName = metric.getYAxis().getColName();
    }

    List<String> dimensionColumns = tableInfo.getDimensionColumnList();
    if (dimensionColumns == null || dimensionColumns.isEmpty()) {
      log.info("No dimension columns configured; skipping chart creation.");
      return;
    }

    final String columnName = metricColName;
    dimensionColumns.stream()
        .filter(colName -> !colName.equalsIgnoreCase(columnName))
        .forEach(colName ->
                     tableInfo.getCProfiles().stream()
                         .filter(cp -> cp.getColName().equalsIgnoreCase(colName))
                         .filter(cp -> !cp.getCsType().isTimeStamp())
                         .findFirst()
                         .ifPresent(cp -> addChartModule(new Metric(tableInfo, cp)))
        );
  }

  private void addChartModule(Metric metricIn) {
    Metric metric = metricIn.copy();
    Object key = model.getKey();
    RunMode runMode = model.getRunMode();
    GroupFunction groupFunction = null;
    ChartKey chartKey = null;

    if (key instanceof ProfileTaskQueryKey ptk) {
      chartKey = new ChartKey(ptk, metric.getYAxis());
      if (RunMode.REALTIME.equals(runMode)) {
        groupFunction = UIState.INSTANCE.getRealtimeGroupFunction(chartKey);
      } else if (RunMode.HISTORY.equals(runMode)) {
        groupFunction = UIState.INSTANCE.getHistoryGroupFunction(chartKey);
      }
    } else if (key instanceof AdHocKey adHocKey) {
      groupFunction = AdHocStateManager.getInstance().getHistoryGroupFunction(adHocKey);
    }

    if (groupFunction != null) {
      metric.setGroupFunction(groupFunction);
    }

    QueryInfo queryInfo = model.getQueryInfo();
    ChartInfo chartInfo = model.getChartInfo();
    TableInfo tableInfo = model.getTableInfo();
    SqlQueryState sqlQueryState = model.getSqlQueryState();
    DStore dStore = model.getDStore();

    IPreviewChart taskPane = chartFactory.create(component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    taskPane.asTaskPane().setTitle(metric.getYAxis().getColName());

    view.setColumnSelected(metric.getYAxis().getColName(), false);

    view.addChartCard(taskPane, (module, error) -> {
      if (error != null) {
        log.error("Failed to load preview chart", error);
        return;
      }
      model.addChartModule(metric.getYAxis(), module);

      if (mode == PreviewMode.PREVIEW && module instanceof IRealTimePreviewChart rt) {
        rt.handleRealTimeRangeUI(rangeRealTime);
      }
      module.handleLegendChange(showLegend ? ChartLegendState.SHOW : ChartLegendState.HIDE);
      module.handleChartConfigState(showConfig ? ChartConfigState.SHOW : ChartConfigState.HIDE);
      module.setCollapsed(!isExpand);
    });
  }
}