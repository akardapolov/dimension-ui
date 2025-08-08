package ru.dimension.ui.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import ru.dimension.ui.component.broker.ParameterStore;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.component.model.DetailState;

public enum UIState {
  INSTANCE;

  private static final String REAL_TIME_METRIC_FUNCTION = "REAL_TIME_METRIC_FUNCTION";
  private static final String HISTORY_METRIC_FUNCTION = "HISTORY_METRIC_FUNCTION";
  private static final String REAL_TIME_RANGE = "REAL_TIME_RANGE";
  private static final String HISTORY_RANGE = "HISTORY_RANGE";
  private static final String REAL_TIME_RANGE_ALL = "REAL_TIME_RANGE_ALL";
  private static final String HISTORY_RANGE_ALL = "HISTORY_RANGE_ALL";
  private static final String SHOW_LEGEND = "SHOW_LEGEND";
  private static final String SHOW_LEGEND_ALL = "SHOW_LEGEND_ALL";
  private static final String SHOW_DETAIL_ALL = "SHOW_DETAIL_ALL";
  private static final String HISTORY_CUSTOM_RANGE = "HISTORY_CUSTOM_RANGE";
  private static final String HISTORY_CUSTOM_RANGE_ALL = "HISTORY_CUSTOM_RANGE_ALL";

  private static final String SHOW_CONFIG = "SHOW_CONFIG";
  private static final String SHOW_CONFIG_ALL = "SHOW_CONFIG_ALL";

  private final ConcurrentMap<String, ParameterStore> globalStateMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<ChartKey, ParameterStore> stateMap = new ConcurrentHashMap<>();

  private final ConcurrentMap<AdHocKey, ParameterStore> adHocStateMap = new ConcurrentHashMap<>();

  public void putRealtimeMetricFunction(ChartKey key, MetricFunction function) {
    getOrCreateParameterStore(key).put(REAL_TIME_METRIC_FUNCTION, function);
  }

  public MetricFunction getRealtimeMetricFunction(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(REAL_TIME_METRIC_FUNCTION, MetricFunction.class) : null;
  }

  public void putHistoryMetricFunction(ChartKey key, MetricFunction function) {
    getOrCreateParameterStore(key).put(HISTORY_METRIC_FUNCTION, function);
  }

  public MetricFunction getHistoryMetricFunction(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(HISTORY_METRIC_FUNCTION, MetricFunction.class) : null;
  }

  public void putRealTimeRange(ChartKey key, RangeRealTime range) {
    getOrCreateParameterStore(key).put(REAL_TIME_RANGE, range);
  }

  public RangeRealTime getRealTimeRange(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(REAL_TIME_RANGE, RangeRealTime.class) : null;
  }

  public void putHistoryRange(ChartKey key, RangeHistory range) {
    getOrCreateParameterStore(key).put(HISTORY_RANGE, range);
  }

  public RangeHistory getHistoryRange(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(HISTORY_RANGE, RangeHistory.class) : null;
  }

  public void putRealTimeRangeAll(String key, RangeRealTime range) {
    getOrCreateParameterGlobalStore(key).put(REAL_TIME_RANGE_ALL, range);
  }

  public RangeRealTime getRealTimeRangeAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(REAL_TIME_RANGE_ALL, RangeRealTime.class) : null;
  }

  public void putHistoryRangeAll(String key, RangeHistory range) {
    getOrCreateParameterGlobalStore(key).put(HISTORY_RANGE_ALL, range);
  }

  public RangeHistory getHistoryRangeAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(HISTORY_RANGE_ALL, RangeHistory.class) : null;
  }

  public void putShowLegend(ChartKey key, Boolean showLegend) {
    getOrCreateParameterStore(key).put(SHOW_LEGEND, showLegend);
  }

  public Boolean getShowLegend(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(SHOW_LEGEND, Boolean.class) : null;
  }

  public void putShowLegendAll(String key, Boolean showLegend) {
    getOrCreateParameterGlobalStore(key).put(SHOW_LEGEND_ALL, showLegend);
  }

  public Boolean getShowLegendAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(SHOW_LEGEND_ALL, Boolean.class) : null;
  }

  public void putShowDetailAll(String key, DetailState showDetail) {
    getOrCreateParameterGlobalStore(key).put(SHOW_DETAIL_ALL, showDetail);
  }

  public DetailState getShowDetailAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(SHOW_DETAIL_ALL, DetailState.class) : null;
  }

  public void putHistoryCustomRange(ChartKey key, ChartRange range) {
    getOrCreateParameterStore(key).put(HISTORY_CUSTOM_RANGE, range);
  }

  public ChartRange getHistoryCustomRange(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(HISTORY_CUSTOM_RANGE, ChartRange.class) : null;
  }

  public void putHistoryCustomRangeAll(String key, ChartRange range) {
    getOrCreateParameterGlobalStore(key).put(HISTORY_CUSTOM_RANGE_ALL, range);
  }

  public ChartRange getHistoryCustomRangeAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(HISTORY_CUSTOM_RANGE_ALL, ChartRange.class) : null;
  }

  public void removeChartState(ChartKey key) {
    stateMap.remove(key);
  }

  private ParameterStore getOrCreateParameterStore(ChartKey key) {
    return stateMap.computeIfAbsent(key, k -> new ParameterStore());
  }

  private ParameterStore getOrCreateParameterGlobalStore(String key) {
    return globalStateMap.computeIfAbsent(key, k -> new ParameterStore());
  }

  public void putHistoryMetricFunction(AdHocKey key, MetricFunction function) {
    getOrCreateParameterStore(key).put(HISTORY_METRIC_FUNCTION, function);
  }

  public MetricFunction getHistoryMetricFunction(AdHocKey key) {
    ParameterStore store = adHocStateMap.get(key);
    return store != null ? store.get(HISTORY_METRIC_FUNCTION, MetricFunction.class) : null;
  }

  public void putHistoryRange(AdHocKey key, RangeHistory range) {
    getOrCreateParameterStore(key).put(HISTORY_RANGE, range);
  }

  public RangeHistory getHistoryRange(AdHocKey key) {
    ParameterStore store = adHocStateMap.get(key);
    return store != null ? store.get(HISTORY_RANGE, RangeHistory.class) : null;
  }

  public void putShowLegend(AdHocKey key, Boolean showLegend) {
    getOrCreateParameterStore(key).put(SHOW_LEGEND, showLegend);
  }

  public Boolean getShowLegend(AdHocKey key) {
    ParameterStore store = adHocStateMap.get(key);
    return store != null ? store.get(SHOW_LEGEND, Boolean.class) : null;
  }

  public void putHistoryCustomRange(AdHocKey key, ChartRange range) {
    getOrCreateParameterStore(key).put(HISTORY_CUSTOM_RANGE, range);
  }

  public ChartRange getHistoryCustomRange(AdHocKey key) {
    ParameterStore store = adHocStateMap.get(key);
    return store != null ? store.get(HISTORY_CUSTOM_RANGE, ChartRange.class) : null;
  }

  public void putShowConfig(ChartKey key, Boolean showConfig) {
    getOrCreateParameterStore(key).put(SHOW_CONFIG, showConfig);
  }

  public Boolean getShowConfig(ChartKey key) {
    ParameterStore store = stateMap.get(key);
    return store != null ? store.get(SHOW_CONFIG, Boolean.class) : null;
  }

  public void putShowConfigAll(String key, Boolean showConfig) {
    getOrCreateParameterGlobalStore(key).put(SHOW_CONFIG_ALL, showConfig);
  }

  public Boolean getShowConfigAll(String key) {
    ParameterStore store = globalStateMap.get(key);
    return store != null ? store.get(SHOW_CONFIG_ALL, Boolean.class) : null;
  }

  private ParameterStore getOrCreateParameterStore(AdHocKey key) {
    return adHocStateMap.computeIfAbsent(key, k -> new ParameterStore());
  }
}