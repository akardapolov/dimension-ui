package ru.dimension.ui.state;

import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.view.RangeHistory;

public class AdHocStateManager {
  private static final AdHocStateManager INSTANCE = new AdHocStateManager();

  private AdHocStateManager() {}

  public static AdHocStateManager getInstance() {
    return INSTANCE;
  }

  public RangeHistory getHistoryRange(AdHocKey adHocKey, String globalKey) {
    RangeHistory local = UIState.INSTANCE.getHistoryRange(adHocKey);
    return local != null ? local : UIState.INSTANCE.getHistoryRangeAll(globalKey);
  }

  public ChartRange getCustomChartRange(AdHocKey adHocKey, String globalKey) {
    ChartRange local = UIState.INSTANCE.getHistoryCustomRange(adHocKey);
    return local != null ? local : UIState.INSTANCE.getHistoryCustomRangeAll(globalKey);
  }

  public Boolean getShowLegend(AdHocKey adHocKey, String globalKey) {
    Boolean local = UIState.INSTANCE.getShowLegend(adHocKey);
    return local != null ? local : UIState.INSTANCE.getShowLegendAll(globalKey);
  }

  public ChartCardState getChartCardStateAll(String globalKey) {
    return UIState.INSTANCE.getChartCardStateAll(globalKey);
  }

  public void putHistoryRange(AdHocKey adHocKey, RangeHistory range) {
    UIState.INSTANCE.putHistoryRange(adHocKey, range);
  }

  public void putGlobalHistoryRange(String globalKey, RangeHistory range) {
    UIState.INSTANCE.putHistoryRangeAll(globalKey, range);
  }

  public void putHistoryCustomRange(AdHocKey adHocKey, ChartRange range) {
    UIState.INSTANCE.putHistoryCustomRange(adHocKey, range);
  }

  public void putGlobalHistoryCustomRange(String key, ChartRange range) {
    UIState.INSTANCE.putHistoryCustomRangeAll(key, range);
  }

  public void putShowLegend(AdHocKey adHocKey, Boolean showLegend) {
    UIState.INSTANCE.putShowLegend(adHocKey, showLegend);
  }

  public void putGlobalShowLegend(String globalKey, Boolean showLegend) {
    UIState.INSTANCE.putShowLegendAll(globalKey, showLegend);
  }

  public void putGlobalChartCardState(String globalKey, ChartCardState cardState) {
    UIState.INSTANCE.putChartCardStateAll(globalKey, cardState);
  }

  public GroupFunction getHistoryGroupFunction(AdHocKey adHocKey) {
    return UIState.INSTANCE.getHistoryGroupFunction(adHocKey);
  }

  public TimeRangeFunction getTimeRangeFunction(AdHocKey adHocKey) {
    return UIState.INSTANCE.getTimeRangeFunction(adHocKey);
  }

  public NormFunction getNormFunction(AdHocKey adHocKey) {
    return UIState.INSTANCE.getNormFunction(adHocKey);
  }

  public void putHistoryGroupFunction(AdHocKey adHocKey, GroupFunction function) {
    UIState.INSTANCE.putHistoryGroupFunction(adHocKey, function);
  }

  public void putTimeRangeFunction(AdHocKey adHocKey, TimeRangeFunction function) {
    UIState.INSTANCE.putTimeRangeFunction(adHocKey, function);
  }

  public void putNormFunction(AdHocKey adHocKey, NormFunction function) {
    UIState.INSTANCE.putNormFunction(adHocKey, function);
  }

  public ChartRange getHistoryCustomRange(AdHocKey adHocKey) {
    return UIState.INSTANCE.getHistoryCustomRange(adHocKey);
  }
}